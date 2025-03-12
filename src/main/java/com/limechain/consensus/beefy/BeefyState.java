package com.limechain.consensus.beefy;

import com.limechain.ServiceConsensusState;
import com.limechain.consensus.beefy.dto.BeefyAuthoritySet;
import com.limechain.consensus.beefy.dto.BeefySession;
import com.limechain.consensus.beefy.dto.message.BeefyConsensusMessage;
import com.limechain.network.protocol.beefy.messages.justification.SignedCommitment;
import com.limechain.network.protocol.beefy.messages.vote.VoteMessage;
import com.limechain.runtime.Runtime;
import com.limechain.state.AbstractState;
import com.limechain.storage.DBConstants;
import com.limechain.storage.KVRepository;
import com.limechain.storage.StateUtil;
import com.limechain.storage.block.state.BlockState;
import com.limechain.storage.crypto.KeyStore;
import com.limechain.storage.crypto.KeyType;
import io.micrometer.common.lang.Nullable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.java.Log;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents the state information required for managing BEEFY finality rounds
 * and validator sets. BEEFY is designed to complement GRANDPA by enabling efficient finality proofs across different
 * blockchain networks.
 * Note: Intended for use only when the host is configured as an Authoring Node.
 */
@Log
@Getter
@Setter
@Component
@RequiredArgsConstructor
public class BeefyState extends AbstractState implements ServiceConsensusState {

    private static final BigInteger THRESHOLD_DENOMINATOR = BigInteger.valueOf(3);
    private static final int MIN_BLOCK_DELTA = 1;

    private BeefyAuthoritySet authoritySet;

    private BigInteger disabledAuthority;

    private final BlockState blockState;
    private final KeyStore keyStore;
    private final KVRepository<String, Object> repository;

    private BigInteger roundNumber;

    @Nullable
    private BigInteger beefyGenesis;

    @Nullable
    private BigInteger beefyFinalized;

    @Nullable
    private BigInteger grandpaFinalized;

    /**
     * Tracks the next block number (digest) for which BEEFY should process votes or finalization.
     * Initialized as the maximum of beefyGenesis and beefyFinalized, or zero if genesis is unknown.
     */
    private BigInteger nextDigest;

    @Nullable
    private BigInteger lastVoted;

    @Nullable
    private VoteMessage lastVote;

    //mapper key is mandatory block number where authority set change appeared
    private LinkedHashMap<BigInteger, BeefySession> sessions = new LinkedHashMap<>();

    @Override
    public void populateDataFromRuntime(Runtime runtime) {
        //Todo: retrieve the validatorSet making call to beefyApi
    }

    @Override
    public void initializeFromDatabase() {
        loadPersistedState();
    }

    @Override
    public void persistState() {
        persistBeefyAuthorities();
        persistAuthoritiesSetId();
        persistRoundNumber(roundNumber);
    }

    public void initializeNextDigest() {
        if (beefyGenesis != null) {
            nextDigest = beefyFinalized != null
                    ? beefyFinalized.max(beefyGenesis)
                    : beefyGenesis;
        } else {
            nextDigest = BigInteger.ZERO;
        }
    }

    private void handleSessionTransitions(BigInteger grandpaFinalized) {
        while (nextDigest.compareTo(grandpaFinalized) <= 0) {
            //Todo: In kagome fetching header.
            Pair<BigInteger, BeefyAuthoritySet> validatorsSet = detectAuthoritySetChange(
                    nextDigest,
                    sessions.isEmpty() ? beefyGenesis : nextDigest
            );

            if (validatorsSet != null) {
                BigInteger sessionBlock = validatorsSet.getLeft();
                BeefyAuthoritySet authoritySet = validatorsSet.getRight();
                org.javatuples.Pair<byte[], byte[]> keyPair = keyStore.findKeyPair(authoritySet.getPublicKeys(), KeyType.BEEFY)
                        .orElse(null);
                if (keyPair == null) {
                    log.info(
                            String.format("BEEFY: We are not chosen to vote in current session, block number: %s",
                                    sessionBlock));
                }
                BeefySession beefySession = new BeefySession(authoritySet, keyPair);
                sessions.put(sessionBlock, beefySession);
            }
            nextDigest = nextDigest.add(BigInteger.ONE);
        }
    }

    /**
     * Checks for validator set changes at a given block and returns a pair of the block and the new set.
     */
    private Pair<BigInteger, BeefyAuthoritySet> detectAuthoritySetChange(BigInteger maxBlockNumber,
                                                                         BigInteger minBlockNumber) {
        //Todo: We should search for authority changes in runtime and beefyValidatorsDigest
        return null;
    }

    /**
     * It checks for mandatory blocks by detecting set changes in all blocks
     * between the last BEEFY finalized and GRANDPA finalized blocks.
     */
    private void handleBeefyAuthorityConsensusMessage(BeefyConsensusMessage consensusMessage, BigInteger currentBlockNumber) {
        switch (consensusMessage.getFormat()) {
            case BEEFY_CHANGED_AUTHORITIES -> {
                //Todo implement handle BEEFY_CHANGED_AUTHORITIES logic.

            }
            case BEEFY_ON_DISABLED -> disabledAuthority = consensusMessage.getDisabledAuthority();
        }
    }

    public void vote() {

        // Get the first session (round)
        Map.Entry<BigInteger, BeefySession> sessionStart = sessions.firstEntry();

        // If no session is found, exit the method
        if (sessionStart == null) {
            log.info("Vote BEEFY: No voting round started");
            return;
        }

        BigInteger sessionStartBlock = sessionStart.getKey();

        // Calculate the target vote block number
        BigInteger targetVoteBlockNumber;

        // If the mandatory block (sessionStart) does not have a beefy justification yet, vote on it
        if (beefyFinalized.compareTo(sessionStartBlock) < 0) {
            log.info(String.format("Vote BEEFY: vote target - mandatory block: #%s%n", sessionStartBlock));
            targetVoteBlockNumber = sessionStartBlock;
        } else {
            BigInteger diff = grandpaFinalized.subtract(beefyFinalized).max(BigInteger.ZERO).add(BigInteger.ONE);
            int diffInt = diff.min(BigInteger.valueOf(Integer.MAX_VALUE)).intValue();
            int nextPowerOfTwo = (Integer.bitCount(diffInt) == 1) ? diffInt : Integer.highestOneBit(diffInt) << 1;
            int adjustedDiff = Math.max(MIN_BLOCK_DELTA, nextPowerOfTwo);

            targetVoteBlockNumber = beefyFinalized.add(BigInteger.valueOf(adjustedDiff));

            log.info(String.format("Vote BEEFY: vote target - diff: %d, next_power_of_two: %d, target block: #%s%n",
                    diffInt, nextPowerOfTwo, targetVoteBlockNumber));
        }

        // Don't vote for targets until they've been finalized (`target` can be > `bestGrandpa` when `minDelta` is big enough).
        // Also, ensure it's not voting on a block that has already been voted on.
        if (targetVoteBlockNumber.compareTo(grandpaFinalized) > 0 || targetVoteBlockNumber.compareTo(lastVoted) <= 0) {
            return; // No voting if target is beyond grandpa finalized or it's not a new block
        }

        // If it's a valid vote target, update the last voted block
        lastVoted = targetVoteBlockNumber;

        // TODO: Get Beefy Keys
        // TODO: Create Commitment and signature
        // TODO: Broadcast Vote Message
    }

    private void reportDoubleVoting(VoteMessage voteMessage) {
        //Todo: Generate key ownership proof
        //Todo: Submit report double voting to Beefy api path
    }

    private void loadPersistedState() {
        BigInteger setId = fetchAuthoritiesSetId();
        List<byte[]> authorities = fetchBeefyAuthorities(setId);
        this.authoritySet = new BeefyAuthoritySet(setId, authorities);
    }

    private BigInteger fetchAuthoritiesSetId() {
        return repository.find(DBConstants.BEEFY_SET_ID, BigInteger.ZERO);
    }

    private void persistAuthoritiesSetId() {
        repository.save(DBConstants.BEEFY_SET_ID, authoritySet.getSetId());
    }


    private List<byte[]> fetchBeefyAuthorities(BigInteger setId) {
        return repository.find(
                StateUtil.generateAuthorityKey(DBConstants.BEEFY_AUTHORITY_SET, setId),
                Collections.emptyList()
        );
    }

    private void persistBeefyAuthorities() {
        repository.save(
                StateUtil.generateAuthorityKey(DBConstants.BEEFY_AUTHORITY_SET, authoritySet.getSetId()),
                authoritySet.getPublicKeys()
        );
    }

    private BigInteger fetchDisabledAuthority(BigInteger setId) {
        return repository.find(
                StateUtil.generateBeefyDisabledAuthorityKey(
                        DBConstants.BEEFY_DISABLED_AUTHORITY, setId
                ),
                BigInteger.ZERO
        );
    }

    private void persistDisabledAuthority() {
        repository.save(
                StateUtil.generateBeefyDisabledAuthorityKey(
                        DBConstants.BEEFY_DISABLED_AUTHORITY, authoritySet.getSetId()
                ),
                disabledAuthority
        );
    }

    private BigInteger fetchBeefyFinalized() {
        return repository.find(DBConstants.BEEFY_FINALIZED, BigInteger.ZERO);
    }

    private void persistBeefyFinalized() {
        repository.save(DBConstants.BEEFY_FINALIZED, beefyFinalized);
    }

    private BigInteger fetchRoundNumber() {
        return repository.find(DBConstants.BEEFY_ROUND, BigInteger.ZERO);
    }

    private void persistRoundNumber(BigInteger roundNumber) {
        repository.save(DBConstants.BEEFY_ROUND, roundNumber);
    }

    private SignedCommitment fetchJustification(BigInteger blockNumber) {
        return repository.find(
                StateUtil.generateBeefyJustificationKey(DBConstants.BEEFY_JUSTIFICATION, blockNumber),
                new SignedCommitment()
        );
    }

    private void persistJustification(BigInteger blockNumber, SignedCommitment justification) {
        repository.save(
                StateUtil.generateBeefyJustificationKey(DBConstants.BEEFY_JUSTIFICATION, blockNumber),
                justification
        );
    }
}

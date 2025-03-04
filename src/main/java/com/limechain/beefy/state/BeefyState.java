package com.limechain.beefy.state;

import com.limechain.ServiceConsensusState;
import com.limechain.beefy.dto.BeefyPayloadId;
import com.limechain.beefy.dto.Commitment;
import com.limechain.beefy.dto.PayloadElement;
import com.limechain.beefy.dto.SignedCommitment;
import com.limechain.beefy.dto.ValidatorSet;
import com.limechain.beefy.dto.VoteMessage;
import com.limechain.exception.beefy.BeefyGenericException;
import com.limechain.network.protocol.beefy.messages.consensus.BeefyConsensusMessage;
import com.limechain.network.protocol.warp.DigestHelper;
import com.limechain.network.protocol.warp.dto.BlockHeader;
import com.limechain.runtime.Runtime;
import com.limechain.state.AbstractState;
import com.limechain.storage.DBConstants;
import com.limechain.storage.KVRepository;
import com.limechain.storage.StateUtil;
import com.limechain.storage.block.state.BlockState;
import com.limechain.storage.crypto.KeyStore;
import io.micrometer.common.lang.Nullable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents the state information required for managing BEEFY finality rounds
 * and validator sets. BEEFY is designed to complement GRANDPA by enabling efficient finality proofs across different
 * blockchain networks.
 * Note: Intended for use only when the host is configured as an Authoring Node.
 */
@Log
@Getter
@Component
@RequiredArgsConstructor
public class BeefyState extends AbstractState implements ServiceConsensusState {

    private static final BigInteger THRESHOLD_DENOMINATOR = BigInteger.valueOf(3);

    private ValidatorSet validatorSet;

    private BigInteger disabledAuthority;
    private byte[] mmrRootHash;

    private final BlockState blockState;
    private final KeyStore keyStore;
    private final KVRepository<String, Object> repository;

    private BigInteger roundNumber;

    @Nullable
    private BigInteger beefyGenesis;

    @Nullable
    private BigInteger beefyFinalized;

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
    private Map<BigInteger, BeefySession> sessions = new ConcurrentHashMap<>();

    //mapper key is authority public key
    private Map<byte[], VoteMessage> signedVotes = new ConcurrentHashMap<>();

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
        persistBeefyValidators();
        persistValidatorsSetId();
        persistRoundNumber(roundNumber);
    }

    private void initializeNextDigest() {
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
            Pair<BigInteger, ValidatorSet> validatorsSet = detectValidatorSetChange(
                    nextDigest,
                    sessions.isEmpty() ? beefyGenesis : nextDigest
            );

            if (validatorsSet != null) {
                BigInteger sessionBlock = validatorsSet.getLeft();
                ValidatorSet validatorSet = validatorsSet.getRight();

                BeefySession beefySession = new BeefySession(validatorSet);
                sessions.put(sessionBlock, beefySession);
            }
            nextDigest = nextDigest.add(BigInteger.ONE);
        }
    }

    /**
     * Checks for validator set changes at a given block and returns a pair of the block and the new set.
     */
    private Pair<BigInteger, ValidatorSet> detectValidatorSetChange(BigInteger maxBlockNumber,
                                                                    BigInteger minBlockNumber) {
        //Todo: We should search for authority changes in runtime and beefyValidatorsDigest
        return null;
    }

    /**
     * The threshold is determined as the numOfValidators - (numOfValidators - 1) / 3
     *
     * @return minimum required validators for finality.
     */
    public BigInteger getThreshold() {
        var validatorSize = validatorSet.getValidators().size();
        if (validatorSize == 0) {
            return BigInteger.ZERO;
        }
        var numOfValidators = BigInteger.valueOf(validatorSize);
        var faulty = (numOfValidators.subtract(BigInteger.ONE)).divide(THRESHOLD_DENOMINATOR);

        return numOfValidators.subtract(faulty);
    }

    private void reportDoubleVoting(VoteMessage voteMessage) {
        //Todo: Generate key ownership proof
        //Todo: Submit report double voting to Beefy api path
    }

    private void loadPersistedState() {
        BigInteger setId = fetchValidatorsSetId();
        List<byte[]> validators = fetchBeefyValidators(setId);
        this.validatorSet = new ValidatorSet(validators, setId);
    }

    private BigInteger fetchValidatorsSetId() {
        return repository.find(DBConstants.BEEFY_SET_ID, BigInteger.ZERO);
    }

    public void persistValidatorsSetId() {
        repository.save(DBConstants.BEEFY_SET_ID, validatorSet.getSetId());
    }


    private List<byte[]> fetchBeefyValidators(BigInteger setId) {
        return repository.find(
                StateUtil.generateAuthorityKey(DBConstants.BEEFY_AUTHORITY_SET, setId),
                Collections.emptyList()
        );
    }

    public void persistBeefyValidators() {
        repository.save(
                StateUtil.generateAuthorityKey(DBConstants.BEEFY_AUTHORITY_SET, validatorSet.getSetId()),
                validatorSet.getValidators()
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

    private Commitment getCommitment(BigInteger blockNumber, BigInteger setId) {
        BlockHeader blockHeader = blockState.getHeaderByNumber(blockNumber);
        byte[] mmrHash = extractMmrRootHash(blockHeader);
        PayloadElement payloadElement = new PayloadElement(BeefyPayloadId.MMR, mmrHash);

        return new Commitment(Collections.singletonList(payloadElement), blockNumber, setId);
    }

    private byte[] extractMmrRootHash(BlockHeader blockHeader) {
        return DigestHelper.getBeefyConsensusMessages(blockHeader.getDigest())
                .stream().map(BeefyConsensusMessage::getMmrRootHash)
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new BeefyGenericException(
                        String.format("No MMR digest found in block header: %s", blockHeader.getBlockNumber()))
                );
    }
}

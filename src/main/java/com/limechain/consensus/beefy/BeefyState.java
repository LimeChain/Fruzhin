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
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

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

    private Deque<BeefySession> sessions = new ArrayDeque<>();


    @Override
    public void populateDataFromRuntime(Runtime runtime) {
        this.authoritySet = runtime.getBeefyValidatorSet().orElseGet(() -> {
            log.warning("BeefyValidatorSet is not available from runtime, setting authoritySet to null.");
            return null;
        });
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

    public void handleBeefyConsensusMessage(BeefyConsensusMessage consensusMessage, BigInteger blockNumber) {
        switch (consensusMessage.getFormat()) {
            case BEEFY_CHANGED_AUTHORITIES -> handleChangedBeefyAuthorities(consensusMessage, blockNumber);
            case BEEFY_ON_DISABLED -> disabledAuthority = consensusMessage.getDisabledAuthority();
        }
    }

    private void handleChangedBeefyAuthorities(BeefyConsensusMessage consensusMessage, BigInteger blockNumber) {
        org.javatuples.Pair<byte[], byte[]> keyPair = keyStore.findKeyPair(
                consensusMessage.getAuthorityPublicKeys(),
                KeyType.BEEFY
        ).orElse(null);

        if (keyPair == null) {
            log.info(
                    String.format("BEEFY: We are not chosen to vote in current session, block number: %s", blockNumber)
            );
        }

        BeefySession beefySession = new BeefySession(
                new BeefyAuthoritySet(consensusMessage.getAuthorityPublicKeys(), consensusMessage.getAuthoritySetId()),
                blockNumber,
                keyPair
        );

        sessions.add(beefySession);
    }

    private void reportDoubleVoting(VoteMessage voteMessage) {
        //Todo: Generate key ownership proof
        //Todo: Submit report double voting to Beefy api path
    }

    private void loadPersistedState() {
        BigInteger setId = fetchAuthoritiesSetId();
        List<byte[]> authorities = fetchBeefyAuthorities(setId);
        this.authoritySet = new BeefyAuthoritySet(authorities, setId);
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

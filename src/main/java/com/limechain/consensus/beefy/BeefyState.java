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
import org.javatuples.Pair;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
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

    // TODO: Remove nextDigest or remove this comment
    /**
     * Tracks the next block number (digest) for which BEEFY should process votes or finalization.
     * Initialized as the maximum of beefyGenesis and beefyFinalized, or zero if genesis is unknown.
     */
    private BigInteger nextDigest;

    @Nullable
    private BigInteger lastVoted;

    // TODO: Remove lastVote or remove this comment
    @Nullable
    private VoteMessage lastVote;

    private Deque<BeefySession> sessions = new ArrayDeque<>();

    private LinkedHashMap<BigInteger, SignedCommitment> pendingJustifications = new LinkedHashMap<>();


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
        persistAuthoritiesSetId();
        persistBeefyAuthorities();
        persistRoundNumber();
        persistBeefyFinalized();
        persistGrandpaFinalized();
        persistLastVoted();
        persistSessions();
    }

    // TODO: Remove initializeNextDigest or remove this comment
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
        Pair<byte[], byte[]> keyPair = keyStore.findKeyPair(
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
        this.roundNumber = fetchRoundNumber();
        this.beefyFinalized = fetchBeefyFinalized();
        this.grandpaFinalized = fetchGrandpaFinalized();
        this.lastVoted = fetchLastVoted();
        this.sessions = fetchSessions();
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

    private BigInteger fetchBeefyFinalized() {
        return repository.find(DBConstants.BEEFY_FINALIZED, BigInteger.ZERO);
    }

    private void persistBeefyFinalized() {
        repository.save(DBConstants.BEEFY_FINALIZED, beefyFinalized);
    }

    private BigInteger fetchGrandpaFinalized() {
        return repository.find(DBConstants.BEEFY_GRANDPA_FINALIZED, BigInteger.ZERO);
    }

    private void persistGrandpaFinalized() {
        repository.save(DBConstants.BEEFY_GRANDPA_FINALIZED, grandpaFinalized);
    }

    private BigInteger fetchBeefyGenesis() {
        return repository.find(DBConstants.BEEFY_GENESIS, null);
    }

    private void persistBeefyGenesis() {
        repository.save(DBConstants.BEEFY_GENESIS, beefyGenesis);
    }

    private BigInteger fetchRoundNumber() {
        return repository.find(DBConstants.BEEFY_ROUND, BigInteger.ZERO);
    }

    private void persistRoundNumber() {
        repository.save(DBConstants.BEEFY_ROUND, roundNumber);
    }

    private BigInteger fetchLastVoted() {
        return repository.find(DBConstants.BEEFY_LAST_VOTED, BigInteger.ZERO);
    }

    private void persistLastVoted() {
        repository.save(DBConstants.BEEFY_LAST_VOTED, lastVoted);
    }

    private Deque<BeefySession> fetchSessions() {
        return repository.find(DBConstants.BEEFY_SESSIONS, new ArrayDeque<>());
    }

    private void persistSessions() {
        repository.save(DBConstants.BEEFY_SESSIONS, sessions);
    }

    private SignedCommitment fetchJustification(BigInteger blockNumber) {
        return repository.find(
                StateUtil.generateBeefyJustificationKey(DBConstants.BEEFY_JUSTIFICATION, blockNumber),
                null
        );
    }

    private void persistJustification(BigInteger blockNumber, SignedCommitment justification) {
        repository.save(
                StateUtil.generateBeefyJustificationKey(DBConstants.BEEFY_JUSTIFICATION, blockNumber),
                justification
        );
    }
}

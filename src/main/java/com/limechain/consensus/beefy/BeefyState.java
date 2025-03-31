package com.limechain.consensus.beefy;

import com.limechain.ServiceConsensusState;
import com.limechain.consensus.beefy.dto.BeefyAuthoritySet;
import com.limechain.consensus.beefy.dto.BeefySession;
import com.limechain.consensus.beefy.dto.message.BeefyConsensusMessage;
import com.limechain.exception.beefy.BeefyGenericException;
import com.limechain.network.protocol.beefy.messages.justification.SignedCommitment;
import com.limechain.network.protocol.beefy.messages.vote.BeefyVoteMessage;
import com.limechain.runtime.Runtime;
import com.limechain.state.AbstractState;
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

    private final BeefyRepository repository;
    private final BlockState blockState;
    private final KeyStore keyStore;

    private BigInteger disabledAuthority;

    private BeefyAuthoritySet authoritySet;

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
    private BeefyVoteMessage lastVote;

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
        repository.saveAuthoritiesSetId(authoritySet);
        repository.saveBeefyAuthorities(authoritySet);
        repository.saveDisabledAuthority(authoritySet, disabledAuthority);
        repository.saveBeefyGenesis(beefyGenesis);
        repository.saveBeefyFinalized(beefyFinalized);
        repository.saveGrandpaFinalized(grandpaFinalized);
        repository.saveLastVoted(lastVoted);
        repository.saveSessions(sessions);
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

    public BigInteger getBeefyFinalized() {
        if (beefyFinalized == null) throw new BeefyGenericException("Beefy finalized is not initialized yet.");
        return beefyFinalized;
    }

    public BigInteger getGrandpaFinalized() {
        if (grandpaFinalized == null) throw new BeefyGenericException("Grandpa finalized is not initialized yet.");
        return grandpaFinalized;
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

    private void reportDoubleVoting(BeefyVoteMessage beefyVoteMessage) {
        //Todo: Generate key ownership proof
        //Todo: Submit report double voting to Beefy api path
    }

    private void loadPersistedState() {
        BigInteger setId = repository.fetchAuthoritiesSetId();
        List<byte[]> authorities = repository.fetchBeefyAuthorities(setId);

        this.authoritySet = new BeefyAuthoritySet(authorities, setId);
        this.disabledAuthority = repository.fetchDisabledAuthority(setId);
        this.beefyGenesis = repository.fetchBeefyGenesis();
        this.beefyFinalized = repository.fetchBeefyFinalized();
        this.grandpaFinalized = repository.fetchGrandpaFinalized();
        this.lastVoted = repository.fetchLastVoted();
        this.sessions = repository.fetchSessions();
    }
}

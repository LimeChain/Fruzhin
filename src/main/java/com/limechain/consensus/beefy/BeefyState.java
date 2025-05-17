package com.limechain.consensus.beefy;

import com.limechain.ServiceConsensusState;
import com.limechain.consensus.beefy.dto.BeefyAuthoritySet;
import com.limechain.consensus.beefy.dto.BeefySession;
import com.limechain.consensus.beefy.dto.message.BeefyConsensusMessage;
import com.limechain.exception.beefy.BeefyGenericException;
import com.limechain.exception.global.ExecutionFailedException;
import com.limechain.network.PeerRequester;
import com.limechain.network.protocol.beefy.BeefyMessageHandler;
import com.limechain.network.protocol.beefy.messages.justification.SignedCommitment;
import com.limechain.network.protocol.beefy.messages.vote.BeefyVoteMessage;
import com.limechain.rpc.server.AppBean;
import com.limechain.runtime.Runtime;
import com.limechain.state.AbstractState;
import com.limechain.storage.block.state.BlockState;
import com.limechain.storage.crypto.KeyStore;
import com.limechain.storage.crypto.KeyType;
import com.limechain.utils.StringUtils;
import io.micrometer.common.lang.Nullable;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.java.Log;
import org.javatuples.Pair;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.Arrays;
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

    //TODO: remove authority set from here
    private BeefyAuthoritySet authoritySet;

    private BigInteger roundNumber;

    @Nullable
    private BigInteger beefyGenesis;

    private BigInteger beefyFinalized = BigInteger.ZERO;

    @Nullable
    private BigInteger grandpaFinalized;

    // TODO: Remove nextDigest or remove this comment
    /**
     * Tracks the next block number (digest) for which BEEFY should process votes or finalization.
     * Initialized as the maximum of beefyGenesis and beefyFinalized, or zero if genesis is unknown.
     */
    private BigInteger nextDigest;

    @Nullable
    private BigInteger lastVoted = BigInteger.ZERO;

    private BigInteger targetVoteBlockNumber = BigInteger.ZERO;

    // TODO: Remove lastVote or remove this comment
    @Nullable
    private BeefyVoteMessage lastVote;

    private Deque<BeefySession> sessions = new ArrayDeque<>();

    private LinkedHashMap<BigInteger, SignedCommitment> pendingJustifications = new LinkedHashMap<>();

    @Override
    public void populateDataFromRuntime(Runtime runtime) {
        this.beefyGenesis = runtime.getBeefyGenesis().orElse(null);
        this.authoritySet = runtime.getBeefyValidatorSet().orElse(null);
    }

    @Override
    public void initializeFromDatabase() {
        loadPersistedState();
    }

    @Override
    @PreDestroy
    public void persistState() {
        if (authoritySet != null) {
            repository.saveAuthoritiesSetId(authoritySet);
            repository.saveBeefyAuthorities(authoritySet);
            repository.saveDisabledAuthority(authoritySet, disabledAuthority);
        }
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
            case BEEFY_CHANGED_AUTHORITIES -> handleChangedBeefyAuthorities(
                    consensusMessage.getAuthorityPublicKeys(),
                    consensusMessage.getAuthoritySetId(),
                    blockNumber
            );
            case BEEFY_ON_DISABLED -> disabledAuthority = consensusMessage.getDisabledAuthority();
        }
    }

    public BigInteger getGrandpaFinalized() {
        if (grandpaFinalized == null) throw new BeefyGenericException("Grandpa finalized is not initialized yet.");
        return grandpaFinalized;
    }

    public void setupPostWarpSync() {
        if (sessions.isEmpty()) {
            log.fine("Beefy state has no sessions.");
            return;
        }

        BeefySession last = sessions.getLast();
        sessions.clear();
        sessions.addLast(last);

        requestJustification(last.getMandatoryBlock());
    }

    public void requestJustification(BigInteger blockNumber) {
        try {
//            AppBean.getBean(PeerRequester.class).makeBeefyJustificationRequest(blockNumber)
//                    .thenAccept(r ->
//                            AppBean.getBean(BeefyMessageHandler.class).handleSignedCommitment(r));
            log.fine(String.format("requestJustification: Requested justification for block %s.", blockNumber));
        } catch (ExecutionFailedException e) {
            log.warning(String.format("requestJustification: Failed request %s", e.getMessage()));
        }
    }

    public void handleChangedBeefyAuthorities(List<byte[]> authorityPublicKeys,
                                               BigInteger authoritySetId,
                                               BigInteger blockNumber) {

        Pair<byte[], byte[]> keyPair = keyStore.findKeyPair(
                authorityPublicKeys,
                KeyType.BEEFY
        ).orElse(null);

        if (keyPair == null) {
            log.info(
                    String.format("BEEFY: We are not chosen to vote in current session, block number: %s", blockNumber)
            );
        }

        BeefySession beefySession = new BeefySession(
                new BeefyAuthoritySet(authorityPublicKeys, authoritySetId),
                blockNumber,
                keyPair
        );

        sessions.add(beefySession);
    }

    private void reportDoubleVoting(BeefyVoteMessage voteMessage) {
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

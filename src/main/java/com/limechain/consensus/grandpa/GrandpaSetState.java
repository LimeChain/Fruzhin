package com.limechain.consensus.grandpa;

import com.limechain.ServiceConsensusState;
import com.limechain.chain.lightsyncstate.LightSyncState;
import com.limechain.chain.lightsyncstate.PendingChange;
import com.limechain.consensus.dto.Authority;
import com.limechain.consensus.grandpa.dto.AuthoritySetChangeTracker;
import com.limechain.consensus.grandpa.dto.GrandpaAuthoritySet;
import com.limechain.consensus.grandpa.dto.SignedVote;
import com.limechain.consensus.grandpa.dto.Vote;
import com.limechain.consensus.grandpa.dto.message.GrandpaConsensusMessage;
import com.limechain.consensus.grandpa.round.GrandpaRound;
import com.limechain.exception.grandpa.GrandpaGenericException;
import com.limechain.network.protocol.warp.dto.BlockHeader;
import com.limechain.runtime.Runtime;
import com.limechain.state.AbstractState;
import com.limechain.storage.DBConstants;
import com.limechain.storage.KVRepository;
import com.limechain.storage.StateUtil;
import com.limechain.storage.block.state.BlockState;
import com.limechain.storage.crypto.KeyStore;
import com.limechain.storage.crypto.KeyType;
import io.emeraldpay.polkaj.types.Hash256;
import io.libp2p.core.crypto.PubKey;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;

/**
 * Represents the state information for the current round and authorities that are needed
 * for block finalization with GRANDPA.
 * Note: Intended for use only when the host is configured as an Authoring Node.
 */
@Log
@Getter
@Component
@RequiredArgsConstructor
public class GrandpaSetState extends AbstractState implements ServiceConsensusState {

    private static final BigInteger THRESHOLD_DENOMINATOR = BigInteger.valueOf(3);
    private static final BigInteger SET_CHANGES_MAX = BigInteger.valueOf(3);

    private GrandpaRound currentGrandpaRound;

    private GrandpaAuthoritySet authoritySet;
    private AuthoritySetChangeTracker authoritySetChangeTracker = new AuthoritySetChangeTracker();
    private BigInteger disabledAuthority;

    private final BlockState blockState;
    private final KeyStore keyStore;
    private final KVRepository<String, Object> repository;
    private final LinkedHashMap<BigInteger, GrandpaAuthoritySet> pastSetChanges = new LinkedHashMap<>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<BigInteger, GrandpaAuthoritySet> eldest) {
            return SET_CHANGES_MAX.compareTo(BigInteger.valueOf(size())) <= 0;
        }
    };

    @Override
    public void populateDataFromRuntime(Runtime runtime) {
        this.authoritySet.setAuthorities(runtime.getGrandpaApiAuthorities());
    }

    @Override
    public void initializeFromDatabase() {
        loadPersistedState();
    }

    // persists data connected to the current round which may not be finalized
    @Override
    public void persistState() {
        saveGrandpaAuthorities();
        saveAuthoritySetId();
        saveLatestRoundNumber(getCurrentGrandpaRound().getRoundNumber());
        savePreCommits(getCurrentGrandpaRound().getRoundNumber());
        savePreVotes(getCurrentGrandpaRound().getRoundNumber());
    }

    // Persisting of the round data should happen when a round is finalized
    // Round 0 from every set is finalized instantly after creation
    public void persistFinalizedRoundState(BigInteger roundNumber) {
        saveLatestRoundNumber(roundNumber);
        savePreCommits(roundNumber);
        savePreVotes(roundNumber);
    }

    // persists set data into the database
    public void persistNewSetState() {
        saveAuthoritySetId();
        saveGrandpaAuthorities();
    }

    /**
     * The threshold is determined as the total weight of authorities
     * subtracted by the weight of potentially faulty authorities (one-third of the total weight minus one).
     *
     * @return threshold for achieving a super-majority vote
     */
    public BigInteger getThreshold(List<Authority> authorities) {
        var totalWeight = getAuthoritiesTotalWeight(authorities);
        var faulty = (totalWeight.subtract(BigInteger.ONE)).divide(THRESHOLD_DENOMINATOR);
        return totalWeight.subtract(faulty);
    }

    public BigInteger getAuthoritiesTotalWeight(List<Authority> authorities) {
        return authorities.stream()
                .map(Authority::getWeight)
                .reduce(BigInteger.ZERO, BigInteger::add);
    }

    public BigInteger derivePrimary(BigInteger roundNumber) {
        var authoritiesCount = BigInteger.valueOf(authoritySet.getAuthorities().size());
        return roundNumber.remainder(authoritiesCount);
    }

    public void startNewSet(List<Authority> authorities) {

        BigInteger setId = (authoritySet != null && authoritySet.getSetId() != null)
                ? authoritySet.getSetId().add(BigInteger.ONE)
                : BigInteger.ONE;

        this.authoritySet = new GrandpaAuthoritySet(setId, authorities);
        this.authoritySetChangeTracker = new AuthoritySetChangeTracker();

        persistNewSetState();
        updateAuthorityStatus();
        log.log(Level.INFO, "Successfully transitioned to authority set id: " + authoritySet.getSetId());
    }

    public void setLightSyncState(LightSyncState initState) {
        authoritySet.setSetId(initState.getGrandpaAuthoritySet().getSetId());
        authoritySet.setAuthorities(initState.getGrandpaAuthoritySet().getAuthorities());
    }

    /**
     * Apply scheduled or forced authority set changes from the queue if present
     *
     * @param blockNumber required to determine if it's time to apply the change
     */
    public boolean handleAuthoritySetChange(BigInteger blockNumber) {
//        AuthoritySetChange changeSetData = pendingSetChanges.peek();
//
//        boolean updated = false;
//        while (changeSetData != null) {
//
//            if (changeSetData.getApplicationBlockNumber().compareTo(blockNumber) > 0) {
//                break;
//            }
//
//            startNewSet(changeSetData.getAuthorities());
//            pendingSetChanges.poll();
//            updated = true;
//
//            pastSetChanges.put(changeSetData.getApplicationBlockNumber(),
//                    new AuthoritySet(this.setId, this.authorities));
//
//            changeSetData = pendingSetChanges.peek();
//        }

//        return updated;
        return false;
    }

    public void handleGrandpaConsensusMessage(GrandpaConsensusMessage consensusMessage, BlockHeader blockHeader) {
        switch (consensusMessage.getFormat()) {
            case GRANDPA_SCHEDULED_CHANGE -> handleForcedAuthoritySetChange(consensusMessage, blockHeader);
            case GRANDPA_FORCED_CHANGE -> handleScheduledAuthoritySetChange(consensusMessage, blockHeader);
            case GRANDPA_ON_DISABLED -> disabledAuthority = consensusMessage.getDisabledAuthority();
            case GRANDPA_PAUSE -> log.log(Level.SEVERE, "'PAUSE' grandpa message not implemented");
            case GRANDPA_RESUME -> log.log(Level.SEVERE, "'RESUME' grandpa message not implemented");
        }

        log.fine(String.format("Updated grandpa set config: %s", consensusMessage.getFormat().toString()));
    }

    private void handleForcedAuthoritySetChange(GrandpaConsensusMessage consensusMessage, BlockHeader blockHeader) {

        try {

            authoritySetChangeTracker.addPendingChange(
                    PendingChange.buildForcedAuthoritySetChange(
                            consensusMessage.getAuthorities(),
                            consensusMessage.getDelay(),
                            blockHeader.getBlockNumber(),
                            blockHeader.getHash(),
                            consensusMessage.getMedialLastFinalized()
                    ),
                    blockState::isDescendantOf);

        } catch (GrandpaGenericException e) {
            log.warning("Error while importing new forced authority set change: " + e.getMessage());
        }
    }

    private void handleScheduledAuthoritySetChange(GrandpaConsensusMessage consensusMessage, BlockHeader blockHeader) {

        try {

            authoritySetChangeTracker.addPendingChange(
                    PendingChange.buildScheduledAuthoritySetChange(
                            consensusMessage.getAuthorities(),
                            consensusMessage.getDelay(),
                            blockHeader.getBlockNumber(),
                            blockHeader.getHash()
                    ),
                    blockState::isDescendantOf);

        } catch (GrandpaGenericException e) {
            log.warning("Error while importing new scheduled authority set change: " + e.getMessage());
        }
    }

    // We keep a maximum of 3 rounds at a time
    public synchronized void addNewGrandpaRound(GrandpaRound grandpaRound) {

        if (currentGrandpaRound != null && currentGrandpaRound.getPrevious() != null) {
            // Setting the previous to null make it
            currentGrandpaRound.getPrevious().setPrevious(null);
        }

        currentGrandpaRound = grandpaRound;
    }

    public GrandpaRound getGrandpaRound(BigInteger roundNumber) {

        GrandpaRound current = currentGrandpaRound;
        while (!current.getRoundNumber().equals(roundNumber)) {

            if (current.getRoundNumber().compareTo(roundNumber) < 0) {
                return null;
            }

            current = current.getPrevious();
            if (current == null) {
                throw new GrandpaGenericException("Target round not found");
            }
        }

        return current;
    }

    public void saveGrandpaAuthorities() {
        repository.save(StateUtil.generateAuthorityKey(
                DBConstants.GRANDPA_AUTHORITY_SET, authoritySet.getSetId()), authoritySet.getAuthorities()
        );
    }

    public Authority[] fetchGrandpaAuthorities() {
        return repository.find(StateUtil.generateAuthorityKey(
                DBConstants.GRANDPA_AUTHORITY_SET, authoritySet.getSetId()), new Authority[0]
        );
    }

    public void saveAuthoritySetId() {
        repository.save(DBConstants.GRANDPA_SET_ID, authoritySet.getSetId());
    }

    public BigInteger fetchAuthoritiesSetId() {
        return repository.find(DBConstants.GRANDPA_SET_ID, BigInteger.ZERO);
    }

    public void saveLatestRoundNumber(BigInteger roundNumber) {
        repository.save(DBConstants.LATEST_ROUND, roundNumber);
    }

    public BigInteger fetchLatestRoundNumber() {
        return repository.find(DBConstants.LATEST_ROUND, BigInteger.ZERO);
    }

    public void savePreVotes(BigInteger roundNumber) {
        GrandpaRound round = getGrandpaRound(roundNumber);
        Map<Hash256, SignedVote> preVotes = round.getPreVotes();
        repository.save(StateUtil.generatePreVotesKey(
                DBConstants.GRANDPA_PREVOTES, roundNumber, authoritySet.getSetId()), preVotes
        );
    }

    public Map<PubKey, Vote> fetchPreVotes(BigInteger roundNumber) {
        return repository.find(StateUtil.generatePreVotesKey(
                DBConstants.GRANDPA_PREVOTES, roundNumber, authoritySet.getSetId()), Collections.emptyMap()
        );
    }

    public void savePreCommits(BigInteger roundNumber) {
        GrandpaRound round = getGrandpaRound(roundNumber);
        Map<Hash256, SignedVote> preCommits = round.getPreCommits();
        repository.save(StateUtil.generatePreCommitsKey(
                DBConstants.GRANDPA_PRECOMMITS, roundNumber, authoritySet.getSetId()), preCommits
        );
    }

    public Map<PubKey, Vote> fetchPreCommits(BigInteger roundNumber) {
        return repository.find(StateUtil.generatePreCommitsKey(
                DBConstants.GRANDPA_PRECOMMITS, roundNumber, authoritySet.getSetId()), Collections.emptyMap()
        );
    }

    public Optional<BigInteger> getAuthorityWeight(Hash256 authorityPublicKey) {
        return authoritySet.getAuthorities().stream()
                .filter(authority -> new Hash256(authority.getPublicKey()).equals(authorityPublicKey))
                .map(Authority::getWeight)
                .findFirst();
    }

    private void loadPersistedState() {
        authoritySet.setSetId(fetchAuthoritiesSetId());
        authoritySet.setAuthorities(Arrays.asList(fetchGrandpaAuthorities()));
    }

    private void updateAuthorityStatus() {
        keyStore.findKeyPair(authoritySet.getAuthorities().stream()
                                .map(Authority::getPublicKey)
                                .toList(),
                        KeyType.GRANDPA)
                .ifPresentOrElse(AbstractState::setAuthorityStatus, AbstractState::clearAuthorityStatus);
    }
}
package com.limechain.consensus.grandpa;

import com.limechain.ServiceConsensusState;
import com.limechain.chain.lightsyncstate.LightSyncState;
import com.limechain.chain.lightsyncstate.PendingChange;
import com.limechain.consensus.dto.Authority;
import com.limechain.consensus.grandpa.dto.AuthoritySetChangeHandler;
import com.limechain.consensus.grandpa.dto.GrandpaAuthoritySet;
import com.limechain.consensus.grandpa.dto.message.GrandpaConsensusMessage;
import com.limechain.consensus.grandpa.round.GrandpaRound;
import com.limechain.exception.grandpa.GrandpaGenericException;
import com.limechain.network.protocol.warp.dto.BlockHeader;
import com.limechain.runtime.Runtime;
import com.limechain.state.AbstractState;
import com.limechain.storage.block.state.BlockState;
import com.limechain.storage.crypto.KeyStore;
import com.limechain.storage.crypto.KeyType;
import io.emeraldpay.polkaj.types.Hash256;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.Arrays;
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

    private GrandpaAuthoritySet authoritySet = new GrandpaAuthoritySet();
    private AuthoritySetChangeHandler authoritySetChangeHandler = new AuthoritySetChangeHandler();
    private BigInteger disabledAuthority;

    private final BlockState blockState;
    private final KeyStore keyStore;
    private final GrandpaSetRepository repository;
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
    @PreDestroy
    public void persistState() {
        GrandpaRound currentRound = getCurrentGrandpaRound();
        BigInteger roundNumber = currentRound != null ? currentRound.getRoundNumber() : null;

        if (roundNumber != null) {
            repository.saveLatestRoundNumber(roundNumber);
        }

        if (authoritySet == null) {
            return;
        }

        repository.saveGrandpaAuthorities(authoritySet);
        repository.saveAuthoritySetId(authoritySet);

        if (roundNumber != null) {
            repository.savePreCommits(authoritySet, currentRound);
            repository.savePreVotes(authoritySet, currentRound);
        }
    }

    // Persisting of the round data should happen when a round is finalized
    // Round 0 from every set is finalized instantly after creation
    public void persistFinalizedRoundState(BigInteger roundNumber) {
        repository.saveLatestRoundNumber(roundNumber);

        GrandpaRound grandpaRound = getGrandpaRound(roundNumber);
        repository.savePreCommits(authoritySet, grandpaRound);
        repository.savePreVotes(authoritySet, grandpaRound);
    }

    // persists set data into the database
    public void persistNewSetState() {
        repository.saveAuthoritySetId(authoritySet);
        repository.saveGrandpaAuthorities(authoritySet);
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

    public void startNewSet(BigInteger effectiveNumber, List<Authority> authorities) {

        BigInteger setId = (authoritySet != null && authoritySet.getSetId() != null)
                ? authoritySet.getSetId().add(BigInteger.ONE)
                : BigInteger.ONE;

        this.authoritySet = new GrandpaAuthoritySet(setId, authorities);
        this.authoritySetChangeHandler = new AuthoritySetChangeHandler();

        persistNewSetState();
        updateAuthorityStatus();

        pastSetChanges.put(effectiveNumber, authoritySet);

        log.log(Level.INFO, "Successfully transitioned to authority set id: " + authoritySet.getSetId());
    }

    public void setLightSyncState(LightSyncState initState) {
        authoritySet.setSetId(initState.getGrandpaAuthoritySet().getSetId());
        authoritySet.setAuthorities(initState.getGrandpaAuthoritySet().getAuthorities());
    }

    public void handleGrandpaConsensusMessage(GrandpaConsensusMessage consensusMessage, BlockHeader blockHeader) {
        switch (consensusMessage.getFormat()) {
            case GRANDPA_SCHEDULED_CHANGE -> addForcedAuthoritySetChange(consensusMessage, blockHeader);
            case GRANDPA_FORCED_CHANGE -> addScheduledAuthoritySetChange(consensusMessage, blockHeader);
            case GRANDPA_ON_DISABLED -> disabledAuthority = consensusMessage.getDisabledAuthority();
            case GRANDPA_PAUSE -> log.log(Level.SEVERE, "'PAUSE' grandpa message not implemented");
            case GRANDPA_RESUME -> log.log(Level.SEVERE, "'RESUME' grandpa message not implemented");
        }

        log.fine(String.format("Updated grandpa set config: %s", consensusMessage.getFormat().toString()));
    }

    private void addForcedAuthoritySetChange(GrandpaConsensusMessage consensusMessage, BlockHeader blockHeader) {

        try {

            authoritySetChangeHandler.addPendingChange(
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

    private void addScheduledAuthoritySetChange(GrandpaConsensusMessage consensusMessage, BlockHeader blockHeader) {

        try {

            authoritySetChangeHandler.addPendingChange(
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

    /**
     * Apply forced authority set changes when a new block is imported
     * if conditions are met . On block import, only forced changes are applicable.
     *
     * @param hash   The block hash.
     * @param number The block number.
     * @return true if a forced authority set change was applied; false otherwise.
     */
    public boolean applyForcedAuthoritySetChange(Hash256 hash, BigInteger number) {

        Optional<PendingChange> forcedChange = Optional.empty();

        try {
            forcedChange = authoritySetChangeHandler.applyForcedChanges(hash, number, blockState::isDescendantOf);
        } catch (GrandpaGenericException e) {
            log.warning("Error while applying forced change: " + e.getMessage());
        }

        if (forcedChange.isPresent()) {

            PendingChange pendingChange = forcedChange.get();
            startNewSet(pendingChange.getEffectiveNumber(), pendingChange.getNextAuthorities());

            return true;
        }

        return false;
    }

    /**
     * Apply forced/scheduled authority set changes when a block is
     * finalized if conditions are met. On finalization, the method
     * first checks for a forced change. If no forced change is found,
     * it then checks for a scheduled change.
     *
     * @param hash   The block hash.
     * @param number The block number.
     * @return true if either a forced or scheduled authority set change was applied; false otherwise.
     */
    public boolean applyAuthoritySetChange(Hash256 hash, BigInteger number) {

        // First try to apply a forced change.
        if (applyForcedAuthoritySetChange(hash, number)) {
            return true;
        }

        Optional<PendingChange> scheduledChange = Optional.empty();
        try {
            scheduledChange =
                    authoritySetChangeHandler.applyScheduledChanges(hash, number, blockState::isDescendantOf);

        } catch (GrandpaGenericException e) {
            log.warning("Error while applying scheduled change: " + e.getMessage());
        }

        if (scheduledChange.isPresent()) {

            PendingChange pendingChange = scheduledChange.get();
            startNewSet(pendingChange.getEffectiveNumber(), pendingChange.getNextAuthorities());

            return true;
        }

        return false;
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

    public Optional<BigInteger> getAuthorityWeight(Hash256 authorityPublicKey) {
        return authoritySet.getAuthorities().stream()
                .filter(authority -> new Hash256(authority.getPublicKey()).equals(authorityPublicKey))
                .map(Authority::getWeight)
                .findFirst();
    }

    private void loadPersistedState() {
        authoritySet.setSetId(repository.fetchAuthoritiesSetId());
        authoritySet.setAuthorities(Arrays.asList(repository.fetchGrandpaAuthorities(authoritySet)));
    }

    private void updateAuthorityStatus() {
        keyStore.findKeyPair(authoritySet.getAuthorities().stream()
                                .map(Authority::getPublicKey)
                                .toList(),
                        KeyType.GRANDPA)
                .ifPresentOrElse(AbstractState::setAuthorityStatus, AbstractState::clearAuthorityStatus);
    }
}
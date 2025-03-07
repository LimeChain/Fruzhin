package com.limechain.grandpa.state;

import com.limechain.ServiceConsensusState;
import com.limechain.chain.lightsyncstate.Authority;
import com.limechain.storage.forktree.ForkTree;
import com.limechain.chain.lightsyncstate.LightSyncState;
import com.limechain.chain.lightsyncstate.PendingChange;
import com.limechain.exception.grandpa.GrandpaGenericException;
import com.limechain.grandpa.round.GrandpaRound;
import com.limechain.grandpa.vote.SignedVote;
import com.limechain.grandpa.vote.Vote;
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
import org.javatuples.Pair;
import org.springframework.stereotype.Component;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
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
    private final BlockState blockState;
    private final KeyStore keyStore;
    private final KVRepository<String, Object> repository;
    private List<Authority> authorities;
    private BigInteger disabledAuthority;
    private BigInteger setId;

    // TODO:
    //  State can hold a value for the current authority set change(inProgressAuthorityChange) that is not applied yet
    //  Before adding new authority set change in any of the queues we should check if the origin block number
    //  of the change is bigger than the application number of the inProgressAuthorityChange
    // TODO
    //  What will happen if we try to add to the queue a change with originBlockNumber smaller than last origin block number
    //  of the change added to the queue? Should we apply it instead of applying the already existing in queue change or
    //  we just ignore it because its getApplicationBlockNumber of the inProgressAuthoritySetChange is bigger than the origin block
    //  number of the new incoming change
    private ForkTree<PendingChange> pendingScheduledChanges = new ForkTree<>();
    private List<PendingChange> pendingForcedChanges = new ArrayList<>();
    private List<Pair<BigInteger, Hash256>[]> authoritySetChanges = new LinkedList<>();

    private GrandpaRound currentGrandpaRound;

    @Override
    public void populateDataFromRuntime(Runtime runtime) {
        this.authorities = runtime.getGrandpaApiAuthorities();
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

    public void saveGrandpaAuthorities() {
        repository.save(StateUtil.generateAuthorityKey(DBConstants.AUTHORITY_SET, setId), authorities);
    }

    public void saveAuthoritySetId() {
        repository.save(DBConstants.SET_ID, setId);
    }

    public void saveLatestRoundNumber(BigInteger roundNumber) {
        repository.save(DBConstants.LATEST_ROUND, roundNumber);
    }

    public void savePreVotes(BigInteger roundNumber) {
        GrandpaRound round = getGrandpaRound(roundNumber);
        Map<Hash256, SignedVote> preVotes = round.getPreVotes();
        repository.save(StateUtil.generatePreVotesKey(DBConstants.GRANDPA_PREVOTES, roundNumber, setId), preVotes);
    }

    public void savePreCommits(BigInteger roundNumber) {
        GrandpaRound round = getGrandpaRound(roundNumber);
        Map<Hash256, SignedVote> preCommits = round.getPreCommits();
        repository.save(StateUtil.generatePreCommitsKey(DBConstants.GRANDPA_PRECOMMITS, roundNumber, setId), preCommits);
    }

    public GrandpaRound getGrandpaRound(BigInteger roundNumber) {

        GrandpaRound current = currentGrandpaRound;
        while (!current.getRoundNumber().equals(roundNumber)) {

            current = current.getPrevious();
            if (current == null) {
                throw new GrandpaGenericException("Target round not found");
            }
        }

        return current;
    }

    private void loadPersistedState() {
        this.setId = fetchAuthoritiesSetId();
        this.authorities = Arrays.asList(fetchGrandpaAuthorities());
    }

    public Authority[] fetchGrandpaAuthorities() {
        return repository.find(StateUtil.generateAuthorityKey(DBConstants.AUTHORITY_SET, setId), new Authority[0]);
    }

    public BigInteger fetchAuthoritiesSetId() {
        return repository.find(DBConstants.SET_ID, BigInteger.ZERO);
    }

    public void setLightSyncState(LightSyncState initState) {
        this.setId = initState.getGrandpaAuthoritySet().getSetId();
        this.authorities = Arrays.asList(initState.getGrandpaAuthoritySet().getCurrentAuthorities());
    }

    public BigInteger fetchLatestRoundNumber() {
        return repository.find(DBConstants.LATEST_ROUND, BigInteger.ZERO);
    }

    public Map<PubKey, Vote> fetchPreVotes(BigInteger roundNumber) {
        return repository.find(StateUtil.generatePreVotesKey(DBConstants.GRANDPA_PREVOTES, roundNumber, setId),
                Collections.emptyMap());
    }

    public Map<PubKey, Vote> fetchPreCommits(BigInteger roundNumber) {
        return repository.find(StateUtil.generatePreCommitsKey(DBConstants.GRANDPA_PRECOMMITS, roundNumber, setId),
                Collections.emptyMap());
    }

    public Optional<BigInteger> getAuthorityWeight(Hash256 authorityPublicKey) {
        return authorities.stream()
                .filter(authority -> new Hash256(authority.getPublicKey()).equals(authorityPublicKey))
                .map(Authority::getWeight)
                .findFirst();
    }

    /**
     * Apply scheduled or forced authority set changes from the queue if present
     *
     * @param blockNumber required to determine if it's time to apply the change
     */
//    public boolean handleAuthoritySetChange(BigInteger blockNumber) {
//        AuthoritySetChange changeSetData = pendingScheduledChanges.peek();
//
//        boolean updated = false;
//        while (changeSetData != null) {
//
//            if (changeSetData.getEnactmentBlockNumber().compareTo(blockNumber) > 0) {
//                break;
//            }
//
//            startNewSet(changeSetData.getAuthorities());
//            pendingScheduledChanges.poll();
//            finishedAuthoritySetChange = changeSetData;
//            updated = true;
//
//            changeSetData = pendingScheduledChanges.peek();
//        }
//
//        return updated;
//    }

    public void startNewSet(List<Authority> authorities) {

        this.setId = setId != null ? setId.add(BigInteger.ONE) : BigInteger.ONE;
        this.authorities = authorities;

        persistNewSetState();

        updateAuthorityStatus();

        if (AbstractState.isActiveAuthority()) {
            BlockHeader lastFinalized = blockState.getHighestFinalizedHeader();

            GrandpaRound initGrandpaRound = new GrandpaRound(
                    currentGrandpaRound,
                    BigInteger.ZERO,
                    setId,
                    authorities,
                    getThreshold(authorities),
                    false,
                    lastFinalized
            );

            initGrandpaRound.setGrandpaGhost(lastFinalized);

            addNewGrandpaRound(initGrandpaRound);

            persistFinalizedRoundState(initGrandpaRound.getRoundNumber());

            BigInteger primaryIndex = derivePrimary(BigInteger.ONE);
            boolean isPrimary = Arrays.equals(authorities.get(primaryIndex.intValueExact()).getPublicKey(),
                    AbstractState.getGrandpaKeyPair().getValue0());

            GrandpaRound grandpaRound = new GrandpaRound(
                    currentGrandpaRound,
                    BigInteger.ONE,
                    setId,
                    authorities,
                    getThreshold(authorities),
                    isPrimary,
                    lastFinalized
            );

            addNewGrandpaRound(grandpaRound);

            log.log(Level.INFO, "Successfully transitioned to authority set id: " + setId);
        }
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
        var authoritiesCount = BigInteger.valueOf(authorities.size());
        return roundNumber.remainder(authoritiesCount);
    }

    // We keep a maximum of 3 rounds at a time
    public synchronized void addNewGrandpaRound(GrandpaRound grandpaRound) {
        if (currentGrandpaRound.getPrevious() != null && currentGrandpaRound.getPrevious().getPrevious() != null) {
            // Setting the previous to null make it
            currentGrandpaRound.getPrevious().setPrevious(null);
        }

        currentGrandpaRound = grandpaRound;
    }

    private void updateAuthorityStatus() {
        Optional<Pair<byte[], byte[]>> keyPair = authorities.stream()
                .map(a -> keyStore.getKeyPair(KeyType.GRANDPA, a.getPublicKey()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();

        keyPair.ifPresentOrElse(AbstractState::setAuthorityStatus, AbstractState::clearAuthorityStatus);
    }

    // Scheduled and Forced Authority Set Changes methods

    /**
     * Revert to a specified block given its hash and block number.
     * Removes all pending scheduled and forced changes that were signaled after the given revert point.
     *
     * @param blockHash
     * @param blockNumber
     */
    public void revert(Hash256 blockHash, BigInteger blockNumber) {
        //TODO: ForkTree should support drainFilter in order this method to be implemented
    }

    /**
     * Returns the next pending change (its canonical hash and block number) in the chain that contains bestHash.
     * It considers both forced and scheduled changes and returns the earliest one.
     *
     * @param bestBlockHash The hash of the best (current) block.
     * @return An Optional pair of (block hash, block number) for the next pending change, if any.
     */
    public Optional<Pair<Hash256, BigInteger>> nextChange(Hash256 bestBlockHash) {
        Pair<Hash256, BigInteger> forced = null;
        for (PendingChange change : pendingForcedChanges) {
            if (blockState.isDescendantOf(change.getCanonHash(), bestBlockHash)) {
                forced = new Pair<>(change.getCanonHash(), change.getCanonHeight());
                break;
            }
        }

        Pair<Hash256, BigInteger> scheduled = null;
        for (ForkTree.ForkTreeNode<PendingChange> node : pendingScheduledChanges.getRoots()) {

            PendingChange change = node.getData();
            if (blockState.isDescendantOf(change.getCanonHash(), bestBlockHash)) {
                scheduled = new Pair<>(change.getCanonHash(), change.getCanonHeight());
                break;
            }
        }

        if (forced != null && scheduled != null) {
            return (forced.getValue1().compareTo(scheduled.getValue1())) < 0 ?
                    Optional.of(forced) :
                    Optional.of(scheduled);
        } else if (forced != null) {
            return Optional.of(forced);
        } else if (scheduled != null) {
            return Optional.of(scheduled);
        } else {
            return Optional.empty();
        }
    }

//    public void handleGrandpaConsensusMessage(GrandpaConsensusMessage consensusMessage,
//                                              BlockHeader header) {
//
//        //TODO: remove first 2 arms when the handleAuthoritySetChanges method is implemented
//        switch (consensusMessage.getFormat()) {
//            case GRANDPA_SCHEDULED_CHANGE -> pendingScheduledChanges.add(new ScheduledAuthoritySetChange(
//                    consensusMessage.getAuthorities(),
//                    header.getHash(),
//                    header.getBlockNumber(),
//                    consensusMessage.getDelay()
//            ));
//            case GRANDPA_FORCED_CHANGE -> pendingForcedChanges.add(new ForcedAuthoritySetChange(
//                    consensusMessage.getAuthorities(),
//                    header.getHash(),
//                    header.getBlockNumber(),
//                    consensusMessage.getDelay(),
//                    consensusMessage.getAdditionalOffset()
//            ));
//            case GRANDPA_ON_DISABLED -> disabledAuthority = consensusMessage.getDisabledAuthority();
//            case GRANDPA_PAUSE -> log.log(Level.SEVERE, "'PAUSE' grandpa message not implemented");
//            case GRANDPA_RESUME -> log.log(Level.SEVERE, "'RESUME' grandpa message not implemented");
//        }
//
//        log.fine(String.format("Updated grandpa set config: %s", consensusMessage.getFormat().toString()));
//    }

    private void addPendingChange(PendingChange pendingChange) throws Exception {

        if (invalidAuthorityList(pendingChange.getNextAuthorities())) {
            throw new Exception("Invalid authority set");
        }

        PendingChange.DelayKind delayKind = pendingChange.getDelayKind();
        if (delayKind == null) {
            throw new Exception("Delay kind is null");
        }

        PendingChange.DelayKindEnum delayKindEnum = delayKind.getKind();

        if (delayKindEnum == null) {
            throw new Exception("Delay kind enum is null");
        } else if (delayKind.getKind() == PendingChange.DelayKindEnum.BEST) {
            addForcedChange(pendingChange);
        } else if (delayKind.getKind() == PendingChange.DelayKindEnum.FINALIZED) {
            addScheduledChange(pendingChange);
        }
    }

    private void addScheduledChange(PendingChange change) {
        //TODO: tree should support importChange() in order to implement this
    }

    private void addForcedChange(PendingChange pendingChange) throws Exception {

        for (PendingChange change : pendingForcedChanges) {
            if (change.getCanonHash().equals(pendingChange.getCanonHash())) {
                throw new Exception("Duplicate authority set change");
            }
            if (blockState.isDescendantOf(change.getCanonHash(), pendingChange.getCanonHash())) {
                throw new Exception("Multiple pending forced authority set changes");
            }
        }

        int idx = 0;
        while (idx < pendingForcedChanges.size()) {
            PendingChange current = pendingForcedChanges.get(idx);
            int cmp = pendingChange.getEffectiveNumber().compareTo(current.getEffectiveNumber());
            if (cmp < 0 || (cmp == 0 && pendingChange.getCanonHeight().compareTo(current.getCanonHeight()) < 0)) {
                break;
            }
            idx++;
        }

        pendingForcedChanges.add(idx, pendingChange);
    }

    private boolean invalidAuthorityList(List<Authority> authorities) {
        return authorities == null || authorities.isEmpty();
    }

    private Iterable<PendingChange> pendingChanges() {
        List<PendingChange> combined = new ArrayList<>();
//        combined.addAll(pendingScheduledChanges.getAll());
        combined.addAll(pendingForcedChanges);
        return combined;
    }

    private Optional<BigInteger> currentLimit(BigInteger min) {
        return pendingScheduledChanges.getRoots().stream()
                .map(ForkTree.ForkTreeNode::getData)
                .map(PendingChange::getEffectiveNumber)
                .filter(effectiveNumber -> effectiveNumber.compareTo(min) >= 0)
                .min(Comparator.naturalOrder());
    }

    private void applyForcedChanges(Hash256 bestBlockHash, BigInteger bestBlockNumber) throws Exception {
        for (PendingChange change : pendingForcedChanges) {
            if (change.getEffectiveNumber().compareTo(bestBlockNumber) > 0) {
                break;
            }

            if (change.getEffectiveNumber().equals(bestBlockNumber) && (bestBlockHash.equals(change.getCanonHash())
                    || blockState.isDescendantOf(change.getCanonHash(), bestBlockHash))) {

                BigInteger medianLastFinalized = change.getDelayKind().getMedianLastFinalized();

                for (ForkTree.ForkTreeNode<PendingChange> forkTreeNode : pendingScheduledChanges.getRoots()) {
                    PendingChange scheduledChange = forkTreeNode.getData();

                    if (scheduledChange.getEffectiveNumber().compareTo(medianLastFinalized) <= 0 &&
                            blockState.isDescendantOf(scheduledChange.getCanonHash(), change.getCanonHash())) {

                        log.info("Not applying forced authority set change at block " +
                                change.getCanonHeight() + " due to pending scheduled change at block " +
                                scheduledChange.getCanonHeight()
                        );

                        throw new Exception("Forced authority set change dependency unsatisfied: " +
                                scheduledChange.getEffectiveNumber());
                    }
                }
                log.info("Applying forced authority set change at block " + change.getCanonHeight());
//                authoritySetChanges.add(new Pair<>(setId, medianLastFinalized);
            }
        }
    }
}
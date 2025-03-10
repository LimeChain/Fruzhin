package com.limechain.grandpa.state;

import com.limechain.chain.lightsyncstate.Authority;
import com.limechain.chain.lightsyncstate.PendingChange;
import com.limechain.exception.forktree.ForkTreeException;
import com.limechain.exception.grandpa.GrandpaGenericException;
import com.limechain.storage.forktree.ForkTree;
import io.emeraldpay.polkaj.types.Hash256;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.javatuples.Pair;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;

@Getter
@Setter
@NoArgsConstructor
public class AuthoritySet {

    private BigInteger setId;
    private List<Authority> authorities;
    private Pair<BigInteger, Hash256>[] authoritySetChanges;

    private ForkTree<PendingChange> pendingScheduledChanges = new ForkTree<>();
    private List<PendingChange> pendingForcedChanges = new ArrayList<>();

    public AuthoritySet(AuthoritySet previousSet, List<Authority> authorities) {

        BigInteger previousSetSetId = previousSet.getSetId();
        if (this.setId != null) {
            this.setId = previousSetSetId.add(BigInteger.ONE);
        } else {
            this.setId = BigInteger.ONE;
        }

        this.authorities = authorities;
        this.pendingScheduledChanges = new ForkTree<>();
        this.pendingForcedChanges = new ArrayList<>();
    }

    /**
     * Returns the next pending change applicable to the given best block hash.
     * This method searches through both the pending forced changes and the pending scheduled changes
     * to find the first change in each list for which the canonical hash is a descendant of the best block hash.
     * Each matching change is represented as a Pair of its canonical hash and canonical height.
     * If both a forced change and a scheduled change are found, the one with the lower canonical height
     * (i.e. the earlier change) is returned. If only one is found, it is returned; if neither is found,
     * an empty Optional is returned.
     */
    public Optional<Pair<Hash256, BigInteger>> nextChange(Hash256 bestBlockHash,
                                                          BiPredicate<Hash256, Hash256> isDescendantOf) {

        Optional<Pair<Hash256, BigInteger>> forcedChange = pendingForcedChanges.stream()
                .filter(change -> isDescendantOf.test(change.getCanonHash(), bestBlockHash)) //TODO: Do we need to filter the forced changes
                .findFirst()
                .map(change -> new Pair<>(change.getCanonHash(), change.getCanonHeight()));

        Optional<Pair<Hash256, BigInteger>> scheduledChange = pendingScheduledChanges.getRoots().stream()
                .map(ForkTree.ForkTreeNode::getData)
                .filter(change -> isDescendantOf.test(change.getCanonHash(), bestBlockHash))
                .findFirst()
                .map(change -> new Pair<>(change.getCanonHash(), change.getCanonHeight()));

        if (forcedChange.isPresent() && scheduledChange.isPresent()) {
            return forcedChange.get().getValue1().compareTo(scheduledChange.get().getValue1()) < 0 ?
                    forcedChange :
                    scheduledChange;
        }

        return forcedChange.isPresent() ? forcedChange : scheduledChange;
    }

    public void addPendingChange(PendingChange pendingChange, BiPredicate<Hash256, Hash256> isDescendantOf)
            throws GrandpaGenericException {

        if (validateAuthorityList(pendingChange.getNextAuthorities())) {
            throw new GrandpaGenericException("Invalid authority set");
        }

        PendingChange.DelayKind delayKind = Optional.ofNullable(pendingChange.getDelayKind())
                .orElseThrow(() -> new GrandpaGenericException("Delay kind is null"));
        PendingChange.DelayKindEnum delayKindEnum = Optional.ofNullable(delayKind.getKind())
                .orElseThrow(() -> new GrandpaGenericException("Delay kind enum is null"));

        switch (delayKindEnum) {
            case BEST -> addForcedChange(pendingChange, isDescendantOf);
            case FINALIZED -> addScheduledChange(pendingChange, isDescendantOf);
        }
    }

    private void addScheduledChange(PendingChange change, BiPredicate<Hash256, Hash256> isDescendantOf)
            throws GrandpaGenericException {

        try {
            pendingScheduledChanges.importNode(change.getCanonHash(), change.getCanonHeight(), change, isDescendantOf);
        } catch (ForkTreeException e) {
            throw new GrandpaGenericException(e.getMessage());
        }
    }

    /**
     * Adds a pending forced change to the list of pending forced changes.
     * The method first verifies that the new pending change does not duplicate an existing change
     * or conflict with any existing change based on the descendant relationship. It then determines
     * the correct insertion point in the sorted list (sorted by effective number and canonical height)
     * using binary search and inserts the new pending change to maintain the sorted order.
     */
    private void addForcedChange(PendingChange pendingChange, BiPredicate<Hash256, Hash256> isDescendantOf)
            throws GrandpaGenericException {

        for (PendingChange change : pendingForcedChanges) {
            if (change.getCanonHash().equals(pendingChange.getCanonHash())) {
                throw new GrandpaGenericException("Duplicate authority set change");
            }
            if (isDescendantOf.test(change.getCanonHash(), pendingChange.getCanonHash())) {
                throw new GrandpaGenericException("Multiple pending forced authority set changes");
            }
        }

        // Create a comparator that orders PendingChange objects by effective number,
        // and in case of a tie, by canonical height.
        Comparator<PendingChange> comparator = Comparator
                .comparing(PendingChange::getEffectiveNumber)
                .thenComparing(PendingChange::getCanonHeight);

        // Use binary search to find the appropriate insertion index in the sorted list.
        int idx = Collections.binarySearch(pendingForcedChanges, pendingChange, comparator);

        // If the index is non-negative, an equivalent pending change already exists in the list
        if (idx >= 0) {
            throw new GrandpaGenericException(
                    "Pending change with the same effective number and canonHeight already exists"
            );
        } else {
            // If binarySearch returns a negative value, invert it to get the correct insertion index.
            // For example, if binarySearch returns -4, then the element should be inserted at index 3.
            idx = (-idx) - 1;
        }

        // Insert a pending change to specific index and right shift all elements in the list with
        // one starting from this index.
        pendingForcedChanges.add(idx, pendingChange);
    }

    //TODO: Probably not needed
    private Iterable<PendingChange> getAllPendingChanges() {
        List<PendingChange> combined = new ArrayList<>();
        combined.addAll(pendingScheduledChanges.getAll());
        combined.addAll(pendingForcedChanges);
        return combined;
    }

    //TODO: Probably not needed
    private Optional<BigInteger> currentLimit(BigInteger min) {
        return pendingScheduledChanges.getRoots().stream()
                .map(ForkTree.ForkTreeNode::getData)
                .map(PendingChange::getEffectiveNumber)
                .filter(effectiveNumber -> effectiveNumber.compareTo(min) >= 0)
                .min(Comparator.naturalOrder());
    }

    //TODO: called on import block from makeAuthoritiesChanges method
    //TODO: Decrease the horizontal complexity of the method
    //TODO: After calling this method a new set should be started and the pending change should be added to the past changes
    private Optional<PendingChange> applyForcedChanges(Hash256 bestBlockHash,
                                       BigInteger bestBlockNumber,
                                       BiPredicate<Hash256, Hash256> isDescendantOf)
            throws GrandpaGenericException {

        for (PendingChange change : pendingForcedChanges) {

            if (change.getEffectiveNumber().compareTo(bestBlockNumber) > 0) {
                break;
            }

            if (change.getEffectiveNumber().equals(bestBlockNumber) && (bestBlockHash.equals(change.getCanonHash())
                    || isDescendantOf.test(change.getCanonHash(), bestBlockHash))) {

                BigInteger medianLastFinalized = change.getDelayKind().getMedianLastFinalized();

                for (ForkTree.ForkTreeNode<PendingChange> forkTreeNode : pendingScheduledChanges.getRoots()) {
                    PendingChange scheduledChange = forkTreeNode.getData();

                    if (scheduledChange.getEffectiveNumber().compareTo(medianLastFinalized) <= 0 &&
                            isDescendantOf.test(scheduledChange.getCanonHash(), change.getCanonHash())) {

                        throw new GrandpaGenericException("Applying forced authority set change at block " +
                                change.getCanonHeight() + " while pending scheduled change at block " +
                                scheduledChange.getCanonHeight() + " exists."
                        );
                    }
                }

                return Optional.of(change);
            }
        }

        return Optional.empty();
    }

    //TODO: called on finalizing block
    private void applyScheduledChanges(Hash256 bestBlockHash,
                                       BigInteger bestBlockNumber,
                                       BiPredicate<Hash256, Hash256> isDescendantOf) {

    }

    //TODO: called on import block from makeAuthoritiesChanges method
    public void enactScheduledChanges() {

    }

    //TODO: Probably not needed
    public void revert(Hash256 blockHash, BigInteger blockNumber) {
        //TODO: ForkTree should support drainFilter in order this method to be implemented
    }

    private boolean validateAuthorityList(List<Authority> authorities) {
        return authorities == null ||
                authorities.isEmpty() ||
                authorities.stream().anyMatch(a -> a.getWeight().equals(BigInteger.ZERO));
    }
}

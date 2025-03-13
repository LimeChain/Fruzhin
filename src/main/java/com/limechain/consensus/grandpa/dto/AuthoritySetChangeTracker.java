package com.limechain.consensus.grandpa.dto;

import com.limechain.chain.lightsyncstate.PendingChange;
import com.limechain.consensus.dto.Authority;
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
public class AuthoritySetChangeTracker {

    private ForkTree<PendingChange> pendingScheduledChanges = new ForkTree<>();
    private List<PendingChange> pendingForcedChanges = new ArrayList<>();
    private List<Pair<BigInteger, BigInteger>> authoritySetChanges = new ArrayList<>();

    public void addPendingChange(PendingChange pendingChange, BiPredicate<Hash256, Hash256> isDescendantOf)
            throws GrandpaGenericException {

        validateAuthorityList(pendingChange.getNextAuthorities());

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

    //TODO: surround this with try catch and if it throws an error -> ?
    //TODO: After calling this method a new set should be started and the pending change should be added to the past changes
    public Optional<PendingChange> applyForcedChanges(Hash256 bestBlockHash,
                                                      BigInteger bestBlockNumber,
                                                      BiPredicate<Hash256, Hash256> isDescendantOf)
            throws GrandpaGenericException {

        for (PendingChange change : pendingForcedChanges) {

            if (change.getEffectiveNumber().compareTo(bestBlockNumber) > 0) {
                break;
            }

            if (change.getEffectiveNumber().equals(bestBlockNumber) && (bestBlockHash.equals(change.getCanonHash())
                    || isDescendantOf.test(change.getCanonHash(), bestBlockHash))) {

                checkForConflictingScheduledChange(change, isDescendantOf);

                return Optional.of(change);
            }
        }

        return Optional.empty();
    }

    private void checkForConflictingScheduledChange(PendingChange change,
                                                    BiPredicate<Hash256, Hash256> isDescendantOf) {

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
    }

    //TODO: called on finalizing block
    //TODO: After calling this method a new set should be started and the pending change should be added to the past changes
    public Optional<PendingChange> applyScheduledChanges(Hash256 finalizedHash,
                                                         BigInteger finalizedNumber,
                                                         BiPredicate<Hash256, Hash256> isDescendantOf) {

        removeInvalidForcedAuthoritySetChanges(finalizedHash, finalizedNumber, isDescendantOf);

        try {

            return pendingScheduledChanges.finalizeNode(
                    finalizedHash,
                    finalizedNumber,
                    isDescendantOf,
                    pendingChange -> pendingChange.getEffectiveNumber().compareTo(finalizedNumber) <= 0
            );

        } catch (ForkTreeException e) {
            throw new GrandpaGenericException(e.getMessage());
        }
    }

    private void removeInvalidForcedAuthoritySetChanges(Hash256 finalizedHash,
                                                        BigInteger finalizedNumber,
                                                        BiPredicate<Hash256, Hash256> isDescendantOf) {

        List<PendingChange> newForcedChanges = new ArrayList<>();

        for (PendingChange forcedChange : pendingForcedChanges) {
            if (forcedChange.getEffectiveNumber().compareTo(finalizedNumber) > 0 &&
                    isDescendantOf.test(finalizedHash, forcedChange.getCanonHash())) {

                newForcedChanges.add(forcedChange);
            }
        }

        pendingForcedChanges = newForcedChanges;
    }

    private void validateAuthorityList(List<Authority> authorities) {
        if (authorities == null ||
                authorities.isEmpty() ||
                authorities.stream().anyMatch(a -> a.getWeight().equals(BigInteger.ZERO))) {

            throw new GrandpaGenericException("Invalid authority set");
        }
    }
}

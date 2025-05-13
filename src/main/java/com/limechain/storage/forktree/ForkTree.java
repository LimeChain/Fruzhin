package com.limechain.storage.forktree;

import com.limechain.exception.forktree.ForkTreeException;
import io.emeraldpay.polkaj.types.Hash256;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

/**
 * The ForkTree class represents a data structure used to manage and track multiple branches.
 * The <code>isDescendentOf</code> predicate is a important parameter used in most methods to verify the ancestry
 * of nodes.
 * @param <T> the type of the data, that will be stored in the nodes
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ForkTree<T> {

    private List<ForkTreeNode<T>> roots = new ArrayList<>();
    private BigInteger bestFinalizedNumber;

    /**
     * Import a new node into the tree and returns boolean if the node becomes root or not
     */
    public boolean importNode(Hash256 hash,
                              BigInteger number,
                              T data,
                              BiPredicate<Hash256, Hash256> isDescendentOf)
            throws ForkTreeException {

        validateNumberExceedsFinalizedNumber(number);

        ForkTreeNode<T> parent = findParentForInsertion(hash, number, isDescendentOf);

        List<ForkTreeNode<T>> childrenList;
        boolean isRoot;
        if (parent != null) {
            childrenList = parent.children;
            isRoot = false;
        } else {
            childrenList = roots;
            isRoot = true;
        }

        // Check for duplicates
        for (ForkTreeNode<T> child : childrenList) {
            if (child.hash.equals(hash)) {
                throw new ForkTreeException("A node with hash " + hash + " already exists.");
            }
        }

        ForkTreeNode<T> newNode = new ForkTreeNode<>(hash, number, data);
        childrenList.add(newNode);

        // Adding first child to a branch may change the depth and rebalancing is required
        if (childrenList.size() == 1) {
            rebalance();
        }

        return isRoot;
    }

    /**
     * Attempts to finalize a node.
     * If no candidate is found, the method returns <code>Optional.empty()</code>.
     */
    public Optional<T> finalizeNode(Hash256 hash,
                                    BigInteger number,
                                    BiPredicate<Hash256, Hash256> isDescendantOf,
                                    Predicate<T> predicate) throws ForkTreeException {

        validateNumberExceedsFinalizedNumber(number);
        Integer candidateIndex = findCandidateIndex(hash, number, isDescendantOf, predicate);
        Optional<T> finalizedData = finalizeCandidateIfPresent(candidateIndex);
        pruneRoots(hash, number, isDescendantOf);

        return finalizedData;
    }

    /**
     * Checks whether a candidate node for finalization is a root.
     * Returns Optional.of(true) if the candidate is a root, Optional.of(false) if it is not,
     * or Optional.empty() if no candidate qualifies.
     */
    public Optional<Boolean> checkIfFinalizationCandidateIsRoot(Hash256 hash,
                                                                BigInteger number,
                                                                BiPredicate<Hash256, Hash256> isDescendantOf,
                                                                Predicate<T> predicate) throws ForkTreeException {

        validateNumberExceedsFinalizedNumber(number);

        for (ForkTreeNode<T> node : getNodes()) {

            if (predicate.test(node.data) && (node.hash.equals(hash) || isDescendantOf.test(node.hash, hash))) {
                checkChildrenForConflictingDescendant(node, hash, number, isDescendantOf);
                return Optional.of(roots.stream().anyMatch(r -> r.hash.equals(node.hash)));
            }
        }

        return Optional.empty();
    }

    public List<T> getAll() {
        List<T> nodeData = new ArrayList<>();
        Iterator<T> iter = iterator();
        iter.forEachRemaining(nodeData::add);
        return nodeData;
    }

    /**
     * Validates that the provided block number exceeds the current best finalized number.
     */
    private void validateNumberExceedsFinalizedNumber(BigInteger nodeNumber) throws ForkTreeException {
        if (Objects.nonNull(bestFinalizedNumber) && nodeNumber.compareTo(bestFinalizedNumber) <= 0) {
            throw new ForkTreeException("Cannot import or finalize a node " + nodeNumber +
                    " that is ancestor of the current finalized block " + bestFinalizedNumber);
        }
    }

    /**
     * Searches for a candidate node that qualifies for finalization.
     */
    private Integer findCandidateIndex(Hash256 hash,
                                       BigInteger number,
                                       BiPredicate<Hash256, Hash256> isDescendantOf,
                                       Predicate<T> predicate) throws ForkTreeException {

        for (int i = 0; i < roots.size(); i++) {

            ForkTreeNode<T> root = roots.get(i);
            if (predicate.test(root.data) && (root.hash.equals(hash) || isDescendantOf.test(root.hash, hash))) {
                checkChildrenForConflictingDescendant(root, hash, number, isDescendantOf);
                return i;
            }
        }

        return null;
    }

    /**
     * Validates that no child of a node would conflict with finalization.
     */
    private void checkChildrenForConflictingDescendant(ForkTreeNode<T> node,
                                                       Hash256 hash,
                                                       BigInteger number,
                                                       BiPredicate<Hash256, Hash256> isDescendantOf)
            throws ForkTreeException {

        for (ForkTreeNode<T> child : node.children) {

            if (child.number.compareTo(number) <= 0 &&
                    (child.hash.equals(hash) || isDescendantOf.test(child.hash, hash))) {

                throw new ForkTreeException(
                        "Finalized descendant of tree node without finalizing its ancestor/s first"
                );
            }
        }
    }

    /**
     *  Finalizes by removing the candidate from the tree, change the roots to its children,
     *  update the best finalized number and extracting its data.
     */
    private Optional<T> finalizeCandidateIfPresent(Integer candidateIndex) {

        if (candidateIndex != null) {
            ForkTreeNode<T> candidate = roots.remove(candidateIndex.intValue());
            T finalizedData = candidate.data;
            roots = candidate.children;
            bestFinalizedNumber = candidate.number;
            return Optional.of(finalizedData);
        }

        return Optional.empty();
    }

    /**
     * Prunes the list of root nodes to retain only those that remain consistent with the finalized block.
     * <p>
     * This method iterates over the current roots and builds a new list by evaluating each node against the finalized block's hash and number.
     * A node is retained if it meets one of the following conditions:
     * <ul>
     *   <li>
     *     Its block number is greater than the finalized block's number and it is a descendant of the finalized block
     *     (i.e. the finalized block is an ancestor of this node).
     *   </li>
     *   <li>
     *     It exactly matches the finalized block (both the block number and hash are equal).
     *   </li>
     *   <li>
     *     It is an ancestor of the finalized block.
     *   </li>
     * </ul>
     * After processing all nodes, the roots list is replaced with the new filtered list, and the best finalized number is updated.
     */
    private void pruneRoots(Hash256 hash, BigInteger number, BiPredicate<Hash256, Hash256> isDescendantOf) {

        List<ForkTreeNode<T>> newRoots = new ArrayList<>();
        for (ForkTreeNode<T> root : roots) {

            boolean retain = (root.number.compareTo(number) > 0 && isDescendantOf.test(hash, root.hash))
                    || (root.number.equals(number) && root.hash.equals(hash))
                    || isDescendantOf.test(root.hash, hash);

            if (retain) {
                newRoots.add(root);
            }
        }

        roots = newRoots;
        bestFinalizedNumber = number;
    }

    /**
     * The list of root nodes is sorted first and then for each level, child nodes are sorted in descending
     * order by the maximum branch depth.
     * This makes the deepest (longest) chains appear first.
     */
    private void rebalance() {
        roots.sort(Comparator.comparingInt((ForkTreeNode<T> n) -> n.getMaxDepth()).reversed());

        ArrayDeque<ForkTreeNode<T>> stack = new ArrayDeque<>(roots);
        while (!stack.isEmpty()) {
            ForkTreeNode<T> node = stack.pop();
            node.children.sort(Comparator.comparingInt((ForkTreeNode<T> n) -> n.getMaxDepth()).reversed());
            stack.addAll(node.children);
        }
    }

    /**
     * Finds the appropriate parent node for inserting a new block into the fork tree.
     *
     * <p>This method iterates over all root nodes in the fork tree and
     * performs the following checks for each root:
     * <ul>
     *   <li>Skips the root if its block number is greater or equal to the new block's number
     *   (i.e., the root is newer or equal in order).</li>
     *   <li>Skips the root if its hash is not an ancestor of the new block's hash, as determined by the
     *       {@code isDescendentOf} predicate.</li>
     * </ul>
     *
     * <p>For each root that passes these checks, it calls {@code findDeepestAncestor} to locate the deepest
     * valid ancestor within that fork. If a valid ancestor is found, it is immediately returned as the parent
     * for insertion.
     *
     * <p>If no suitable parent is found among the roots (i.e., the method returns {@code null}), it implies
     * that the new block is not a descendant of any existing block, and thus should be added as a new root
     * node in the fork tree.
     */
    private ForkTreeNode<T> findParentForInsertion(Hash256 newHash,
                                                   BigInteger newNumber,
                                                   BiPredicate<Hash256, Hash256> isDescendentOf) {

        for (ForkTreeNode<T> root : roots) {

            if (root.number.compareTo(newNumber) >= 0) continue;
            if (!isDescendentOf.test(root.hash, newHash)) continue;

            return findDeepestAncestor(root, newHash, newNumber, isDescendentOf);
        }

        return null;
    }

    /**
     * Finds the deepest ancestor node in the fork tree that qualifies as an ancestor of a new block,
     * based on a given isDescendantOf predicate and block number.
     * <p>This method performs an iterative depth-first search (DFS) starting from the provided root node,
     * using a stack to avoid recursion. For each node processed, it checks its children and selects the first
     * child that meets two conditions:
     * <ul>
     *   <li>The child's block number is less than the new block's number (newNumber).</li>
     *   <li>The child's hash is an ancestor of the new block's hash (newHash), as determined by the
     *       {@code isDescendantOf} predicate.</li>
     * </ul>
     * When a child satisfies these conditions, it becomes the new candidate, and the search continues down
     * that branch. Since only one child per level is expected to pass the check, the loop breaks early after
     * finding the matching child.
     */
    private ForkTreeNode<T> findDeepestAncestor(ForkTreeNode<T> root,
                                                Hash256 newHash,
                                                BigInteger newNumber,
                                                BiPredicate<Hash256, Hash256> isDescendantOf) {

        ForkTreeNode<T> candidate = root;
        ArrayDeque<ForkTreeNode<T>> stack = new ArrayDeque<>();
        stack.push(root);

        while (!stack.isEmpty()) {
            ForkTreeNode<T> node = stack.pop();

            for (ForkTreeNode<T> child : node.children) {

                // Since we are searching for ancestor, number of the node should be smaller of new number
                if (child.number.compareTo(newNumber) < 0 && isDescendantOf.test(child.hash, newHash)) {
                    candidate = child;
                    stack.push(child);
                    break;
                }
            }
        }

        return candidate;
    }

    /**
     * Searches through the roots for a node with the given hash. If found, finalizes it by removing it
     * from the roots (using finalizeRootAt) and returns its data.
     */
    private Optional<T> finalizeRoot(Hash256 hash) {
        for (int i = 0; i < roots.size(); i++) {
            if (roots.get(i).hash.equals(hash)) {
                return finalizeRootAt(i);
            }
        }
        return Optional.empty();
    }

    /**
     * Finalizes the root node at the given index.
     * This method removes the node from roots, replaces the entire roots list with the node's children,
     * updates bestFinalizedNumber, and returns the node's data
     */
    private Optional<T> finalizeRootAt(int index) {

        if (index >= roots.size()) {
            return Optional.empty();
        }

        ForkTreeNode<T> node = roots.remove(index);
        roots = new ArrayList<>(node.children);
        bestFinalizedNumber = node.number;
        return Optional.of(node.data);
    }

    /**
     * Returns an iterator that traverses the tree in breadth-first order.
     */
    private Iterator<T> iterator() {

        List<T> result = new ArrayList<>();
        ArrayDeque<ForkTreeNode<T>> queue = new ArrayDeque<>(roots);

        while (!queue.isEmpty()) {
            ForkTreeNode<T> current = queue.poll();

            T data = current.data;
            if (data != null) result.add(data);

            queue.addAll(current.children);
        }

        return result.iterator();
    }

    /**
     * Returns a list of all nodes including roots and their children
     */
    private List<ForkTreeNode<T>> getNodes() {

        List<ForkTreeNode<T>> rootsCopy = new ArrayList<>(roots);
        Collections.reverse(rootsCopy);

        // In order to keep the ordering roots and children lists should be reversed
        // because stack is used
        ArrayDeque<ForkTreeNode<T>> stack = new ArrayDeque<>(rootsCopy);
        List<ForkTreeNode<T>> nodes = new ArrayList<>();

        while (!stack.isEmpty()) {
            ForkTreeNode<T> current = stack.pop();
            nodes.add(current);

            List<ForkTreeNode<T>> childrenCopy = new ArrayList<>(current.children);
            Collections.reverse(childrenCopy);
            stack.addAll(childrenCopy);
        }

        return nodes;
    }

    @Getter
    @AllArgsConstructor
    public static class ForkTreeNode<T> {

        private final Hash256 hash;
        private final BigInteger number;
        private final T data;
        private final List<ForkTreeNode<T>> children;

        public ForkTreeNode(Hash256 hash, BigInteger number, T data) {
            this(hash, number, data, new ArrayList<>());
        }

        /**
         * Compute the maximum depth in this node's subtree.
         * A leaf node has depth 1.
         */
        public int getMaxDepth() {
            int max = 0;
            for (ForkTreeNode<T> child : children) {
                max = Math.max(max, child.getMaxDepth());
            }
            return max + 1;
        }
    }
}

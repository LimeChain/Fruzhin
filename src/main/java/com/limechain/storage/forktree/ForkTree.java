package com.limechain.storage.forktree;

import com.limechain.exception.forktree.DuplicateException;
import com.limechain.exception.forktree.RevertException;
import com.limechain.exception.forktree.UnfinalizedAncestor;
import io.emeraldpay.polkaj.types.Hash256;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ForkTree<T> {

    private List<ForkTreeNode<T>> roots = new ArrayList<>();
    private Optional<BigInteger> bestFinalizedNumber = Optional.empty();

    /**
     * Import a new node into the tree
     *
     * @param hash           The block hash of the new node
     * @param number         The block number of the new node
     * @param data           Associated data
     * @param isDescendentOf Function that checks ancestry
     * @return true if the imported node is a root; false if it was appended to a branch
     * @throws Exception if the new node's number is not greater than best finalized number,
     *                   or if a node with the same hash is already present
     */
    //TODO: questionable if we are gonna use the boolean returned value
    public boolean importNode(Hash256 hash,
                              BigInteger number,
                              T data,
                              BiPredicate<Hash256, Hash256> isDescendentOf) throws Exception {

        if (bestFinalizedNumber.isPresent() && number.compareTo(bestFinalizedNumber.get()) <= 0) {
            throw new RevertException("Block number " + number +
                    " is not greater than best finalized number " + bestFinalizedNumber);
        }

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
                throw new DuplicateException("A node with hash " + hash + " already exists.");
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

    public void finalizeWithDescendentIf(Hash256 hash,
                                         BigInteger number,
                                         BiPredicate<Hash256, Hash256> isDescendantOf,
                                         Predicate<T> predicate) throws RevertException, UnfinalizedAncestor {

        if (bestFinalizedNumber.isPresent() && number.compareTo(bestFinalizedNumber.get()) <= 0) {
            throw new RevertException("New block number " + number +
                    " is not greater than best finalized number " + bestFinalizedNumber);
        }


        Integer position = null;
        int index = 0;
        for (ForkTreeNode<T> root : roots) {

            if (predicate.test(root.data) &&
                    (root.hash.equals(hash) || isDescendantOf.test(root.hash, hash))) {

                for (ForkTreeNode<T> child : root.children) {

                    if (child.number.compareTo(number) <= 0 &&
                            (child.hash == hash || isDescendantOf.test(child.hash, hash))) {

                        throw new UnfinalizedAncestor(
                                "Finalized descendent of Tree node without finalizing its ancestor/s first"
                        );
                    }

                    position = index;
                    break;
                }

                index++;
            }
        }

        ForkTreeNode<T> node = null;
        if (position != null) {
            node = roots.get(position);
            roots.remove(node);
            roots = node.children;
            bestFinalizedNumber = Optional.of(node.number);
        }

        boolean changed = false;

        //TODO: refactor because currently the roots are updated while the code iterates over them which is considered as bad practice
        for (ForkTreeNode<T> root : roots) {
            //TODO find a way to make it straight forward
            boolean retain = root.number.compareTo(number) > 0 &&
                    isDescendantOf.test(hash, root.hash) ||
                    root.number.equals(number) &&
                            root.hash.equals(hash) ||
                    isDescendantOf.test(root.hash, hash);

            if (retain) {
                roots.add(root);
            } else {
                changed = true;
            }
        }

        bestFinalizedNumber = Optional.of(number);

        if (node != null) {
            //FINALIZATION RESULT -> CHANGED (node_data)
        } else if (changed){
            //FINALIZATION RESULT -> CHANGED (node)
        } else {
            //FINALIZATION RESULT -> UNCHANGED
        }

//        Optional<T> finalizedRoot = finalizeRoot(hash);
//        if (finalizedRoot.isPresent() && predicate.test(finalizedRoot.get())) {
//            // TODO changed
//        }
//
//        boolean changed = false;
//        int idx = 0;
//        while (idx < roots.size()) {
//            ForkTreeNode<T> root = roots.get(idx);
//
//            boolean isFinalized = root.hash.equals(hash);
//            boolean isDescendant = !isFinalized &&
//                    root.number.compareTo(number) > 0 &&
//                    isDescendantOf.test(root.hash, hash);
//            boolean isAncestor = !isFinalized &&
//                    !isDescendant &&
//                    root.number.compareTo(number) < 0 &&
//                    isDescendantOf.test(root.hash, hash);
//
//            //TODO: This check looks redundant
//            if (isFinalized && predicate.test(root.data)) {
//                finalizeRootAt(idx);
//                //TODO: changed
//            }
//
//            if (isDescendant) {
//                idx++;
//                continue;
//            }
//
//            if (isAncestor) {
//                ForkTreeNode<T> removedNode = roots.remove(idx);
//                roots.addAll(removedNode.children);
//                changed = true;
//                continue;
//            }
//
//            roots.remove(idx);
//            changed = true;
//        }
//
//        bestFinalizedNumber = Optional.of(number);
//        if (changed) {
//            //TODO changed
//        } else {
//            //TODO unchanged
//        }
    }

    /**
     * Returns an iterator that traverses the tree in breadth-first order.
     */
    public Iterator<T> iterator() {
        ArrayDeque<ForkTreeNode<T>> queue = new ArrayDeque<>(roots);
        List<T> result = new ArrayList<>();

        while (!queue.isEmpty()) {
            ForkTreeNode<T> current = queue.poll();

            T data = current.data;
            if (data != null) {
                result.add(data);
            }
            queue.addAll(current.children);
        }

        return result.iterator();
    }

    /**
     * For each level, child nodes are sorted in descending order by the maximum branch depth.
     * This makes the deepest (longest) chains appear first.
     */
    public void rebalance() {
        // Sort roots
        roots.sort(Comparator.comparingInt((ForkTreeNode<T> n) -> n.getMaxDepth()).reversed());

        // Traverse and sort all children
        Deque<ForkTreeNode<T>> stack = new ArrayDeque<>(roots);
        while (!stack.isEmpty()) {
            ForkTreeNode<T> node = stack.pop();
            node.children.sort(Comparator.comparingInt((ForkTreeNode<T> n) -> n.getMaxDepth()).reversed());
            stack.addAll(node.children);
        }
    }

    //TODO: do we need an iterator or just the tree as list?

    /**
     * Finds the candidate parent (if any) in the tree to which a new node should be attached.
     * Searches through all roots and returns the deepest ancestor for which the descendant check successes.
     */
    public ForkTreeNode<T> findParentForInsertion(Hash256 newHash,
                                                  BigInteger newNumber,
                                                  BiPredicate<Hash256, Hash256> isDescendentOf) {

        ForkTreeNode<T> candidate = null;
        for (ForkTreeNode<T> root : roots) {
            ForkTreeNode<T> found = findDeepestAncestor(root, newHash, newNumber, isDescendentOf);

            // Search for the deepest ancestor node
            if (found != null && (candidate == null || found.getMaxDepth() > candidate.getMaxDepth())) {
                candidate = found;
            }
        }

        return candidate;
    }

    /**
     * Recursively searches for the deepest node (in a given branch) that is an ancestor of the new node.
     * A node qualifies if its numer is less than the new node's number and
     * isDescendantOf returns true.
     */
    public ForkTreeNode<T> findDeepestAncestor(ForkTreeNode<T> node,
                                               Hash256 newHash,
                                               BigInteger newNumber,
                                               BiPredicate<Hash256, Hash256> isDescendantOf) {

        // Only consider nodes with a number less than newNumber
        if (node.number.compareTo(newNumber) >= 0) {
            return null;
        }

        if (!isDescendantOf.test(node.hash, newHash)) {
            return null;
        }

        // Search for deeper ancestors
        ForkTreeNode<T> candidate = node;
        for (ForkTreeNode<T> child : node.children) {
            ForkTreeNode<T> deeper = findDeepestAncestor(child, newHash, newNumber, isDescendantOf);
            if (deeper != null) {
                candidate = deeper;
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
                return Optional.of(finalizeRootAt(i));
            }
        }
        return Optional.empty();
    }

    /**
     * Finalizes the root node at the given index.
     * This method removes the node from roots, replaces the entire roots list with the node's children,
     * updates bestFinalizedNumber, and returns the node's data
     */
    private T finalizeRootAt(int index) {
        ForkTreeNode<T> node = roots.remove(index);
        roots = new ArrayList<>(node.children);
        bestFinalizedNumber = Optional.of(node.number);
        return node.data;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ForkTreeNode<T> {
        private Hash256 hash;
        private BigInteger number;
        private T data;
        private List<ForkTreeNode<T>> children;

        public ForkTreeNode(Hash256 hash, BigInteger number, T data) {
            this.hash = hash;
            this.number = number;
            this.data = data;
            this.children = new ArrayList<>();
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

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
    private Optional<BigInteger> bestFinalizedNumber = Optional.empty(); //TODO: check if we can make that BigInteger

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
     * The list of root nodes is sorted first and then for each level, child nodes are sorted in descending
     * order by the maximum branch depth.
     * This makes the deepest (longest) chains appear first.
     */
    public void rebalance() {
        roots.sort(Comparator.comparingInt((ForkTreeNode<T> n) -> n.getMaxDepth()).reversed());

        Deque<ForkTreeNode<T>> stack = new ArrayDeque<>(roots);
        while (!stack.isEmpty()) {
            ForkTreeNode<T> node = stack.pop();
            node.children.sort(Comparator.comparingInt((ForkTreeNode<T> n) -> n.getMaxDepth()).reversed());
            stack.addAll(node.children);
        }
    }

    /**
     * Finds the appropriate parent node for inserting a new block into the fork tree.
     *
     * <p>This method iterates over all root nodes in the fork tree (stored in {@code roots}) and
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
    public ForkTreeNode<T> findParentForInsertion(Hash256 newHash,
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
    public ForkTreeNode<T> findDeepestAncestor(ForkTreeNode<T> root,
                                               Hash256 newHash,
                                               BigInteger newNumber,
                                               BiPredicate<Hash256, Hash256> isDescendantOf) {

        ForkTreeNode<T> candidate = root;
        Deque<ForkTreeNode<T>> stack = new ArrayDeque<>();
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

package com.limechain.trie.structure;

import com.limechain.trie.structure.nibble.Nibble;
import com.limechain.trie.structure.nibble.Nibbles;
import com.limechain.trie.structure.nibble.NibblesCollector;
import com.limechain.trie.structure.slab.Slab;
import com.limechain.trie.structure.slab.exceptions.InvalidSlabIndexException;
import org.javatuples.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Optional;
import java.util.Queue;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class TrieStructure<T> {
    /**
     * The actual container for the trie's nodes.
     * Using a Slab guarantees consistency of node indices without the need for internal management.
     */
    Slab<TrieNode<T>> nodes;

    /**
     * Index of the root node within {@link TrieStructure#nodes}.
     * Null if the trie is empty.
     */
    @Nullable
    Integer rootIndex;

    public TrieStructure() {
        this.nodes = new Slab<>();
        this.rootIndex = null;
    }

    public TrieStructure(int initialCapacity) {
        this.nodes = new Slab<>(initialCapacity);
        this.rootIndex = null;
    }

    /**
     * @return true if the trie is empty
     */
    public boolean isEmpty() {
        return this.nodes.isEmpty();
    }

    /**
     * @return the number of nodes in the trie structure, both
     *         branch (purely structural, with no storage value) and
     *         storage nodes (leafs or branches).
     */
    public int size() {
        return this.nodes.size();
    }

    public Optional<NodeHandle<T>> getRootNode() {
        if (this.rootIndex == null) {
            return Optional.empty();
        }

        return Optional.of(this.nodeHandleAtIndexInner(this.rootIndex));
    }

    /**
     * Queries the trie at the given key.
     * @param key to look up at
     * @return the {@link Entry} at the given key
     */
    //NOTE: maybe rename to `entryAtKey` or `getEntryAtKey` something similar... to align with `getNodeAtIndex`?
    public Entry<T> node(Nibbles key) {
        return switch (this.existingNodeInner(key)) {
            case ExistingNodeInnerResult.Found found ->
                NodeHandle.<T>getConstructor(found.hasStorageValue()).apply(this, found.nodeIndex());
            case ExistingNodeInnerResult.NotFound notFound -> {
                Integer closestAncestorIndex = null;

                if (notFound.closestAncestor() != null) {
                    closestAncestorIndex = notFound.closestAncestor().index();
                }

                yield new Vacant<>(this, key, closestAncestorIndex);
            }
        };
    }

    /**
     * @return an iterator of all {@link TrieNode}s in no specific order,
     *         indexed by their respective {@link TrieNodeIndex}es.
     */
    public Iterator<TrieNodeIndex> iteratorUnordered() {
        return this.streamUnordered().iterator();
    }

    /**
     * Convenience alternative for {@link TrieStructure#iteratorUnordered()}
     * @return the trie structure, represented as an iterable of {@link TrieNodeIndex}es in no specific order.
     * @see TrieStructure#iteratorUnordered()
     */
    public Iterable<TrieNodeIndex> asIterableUnordered() {
        return this::iteratorUnordered;
    }

    /**
     * @return a stream of all {@link TrieNode}s in no specific order,
     *           indexed by their respective {@link TrieNodeIndex}es.
     */
    public Stream<TrieNodeIndex> streamUnordered() {
        return StreamSupport.stream(this.nodes.spliterator(), false)
            .map(Pair::getValue0)
            .map(TrieNodeIndex::new);
    }

    /**
     * @return an iterator of all {@link TrieNode}s in lexicographic order,
     *         indexed by their respective {@link TrieNodeIndex}es.
     */
    public Iterator<TrieNodeIndex> iteratorOrdered() {
        return this.streamOrdered().iterator();
    }

    /**
     * Convenience alternative for {@link TrieStructure#iteratorOrdered()}
     * @return the trie structure, represented as an iterable of {@link TrieNodeIndex}es in lexicographic order.
     * @see TrieStructure#iteratorOrdered()
     */
    public Iterable<TrieNodeIndex> asIterableOrdered() {
        return this::iteratorOrdered;
    }

    /**
     * @return a stream of all {@link TrieNode}s in lexicographic order,
     *           indexed by their respective {@link TrieNodeIndex}es.
     */
    public Stream<TrieNodeIndex> streamOrdered() {
        return this
            .allNodesInLexicographicOrder()
            .map(TrieNodeIndex::new);
    }


    /**
     * @return a stream of all {@link TrieNode}s in lexicographic order,
     *         indexed by their respective integer indices.
     */
    Stream<Integer> allNodesInLexicographicOrder() {
        return Stream.iterate(
            this.rootIndex,
            Objects::nonNull,
            nodeIndex -> {
                TrieNode<T> node = this.getNodeAtIndexInner(nodeIndex);
                // Search for first direct child (as lexicographically next)
                {
                    Optional<Integer> firstChild = node.firstChild();

                    if (firstChild.isPresent()) {
                        return firstChild.get();
                    }
                }

                // If no children, then search for direct siblings
                {
                    Integer nextSibling = this.nextSibling(nodeIndex);
                    if (nextSibling != null) {
                        return nextSibling;
                    }
                }

                // If no direct siblings either, then go up the tree and repeat for parent
                TrieNode.Parent parent = node.parent;
                while (parent != null) {
                    int idx = parent.parentNodeIndex();
                    Integer nextSibling = this.nextSibling(idx);

                    if (nextSibling != null) {
                        return nextSibling;
                    }

                    parent = this.getNodeAtIndexInner(idx).parent;
                }

                // At the end, no nodes should be left.
                return null;
            }
        );
    }

    /**
     * @param nodeIndex index of a node
     * @return the index of the next sibling of the node,
     *         null if it's the last (lexicographically speaking) child if its parent, thus no next sibling exists.
     */
    private @Nullable Integer nextSibling(int nodeIndex) {
        var parentIndexPair = this.getNodeAtIndexInner(nodeIndex).parent;

        if (parentIndexPair == null) {
            return null;
        }

        int parentIndex = parentIndexPair.parentNodeIndex();
        Nibble childIndex = parentIndexPair.childIndexWithinParent();

        var parent = this.getNodeAtIndexInner(parentIndex);

        for (int idx = childIndex.asInt() + 1; idx < 16; ++idx) {
            if (parent.childrenIndices[idx] != null) {
                return parent.childrenIndices[idx];
            }
        }

        return null;
    }

    /**
     * @param key the full key as an iterator of nibbles
     * @return the optional node handle for the given key, or empty if no node corresponds to the given key.
     */
    public Optional<NodeHandle<T>> existingNode(Nibbles key) {
        return switch (this.existingNodeInner(key)) {
            case ExistingNodeInnerResult.Found found -> Optional.of(
                    NodeHandle
                        .<T>getConstructor(found.hasStorageValue())
                        .apply(this, found.nodeIndex())
            );
            case ExistingNodeInnerResult.NotFound ignored -> Optional.empty();
        };
    }

    private ExistingNodeInnerResult existingNodeInner(final Nibbles key) {
        if (this.rootIndex == null) {
            return new ExistingNodeInnerResult.NotFound(null);
        }

        int currentIndex = this.rootIndex;

        assert this.getNodeAtIndexInner(currentIndex).parent == null : "Root's parent index must be null.";

        ExistingNodeInnerResult.NotFound.ClosestAncestor closestAncestor = null;

        Iterator<Nibble> keyIter = key.iterator();
        while (true) {
            TrieNode<T> current = this.nodes.get(currentIndex);

            // First, we must remove `current`'s partial key from `key`, making sure that they
            // match.
            for (Nibble nibble : current.partialKey) {
                if (!keyIter.hasNext() || !keyIter.next().equals(nibble)) {
                    return new ExistingNodeInnerResult.NotFound(closestAncestor);
                }
            }

            // At this point, the tree traversal cursor (the already consumed part of the `key` iterator)
            // exactly matches `current`.

            // NOTE:
            //  For now, the remainingKey argument of `ClosestAncestor` is not used, so if this becomes an efficiency issue
            //  We could omit storing it. Also, this is the only reason we're reassigning the iterator.
            var remainingKey = Nibbles.of(keyIter);
            closestAncestor = new ExistingNodeInnerResult.NotFound.ClosestAncestor(currentIndex, remainingKey);
            keyIter = remainingKey.iterator();

            // If no next nibble is present in the key, return successfully...
            if (!keyIter.hasNext()) {
                return new ExistingNodeInnerResult.Found(currentIndex, current.hasStorageValue);
            }

            // ... otherwise, parse the next nibble as `childIndex`
            Nibble childIndex = keyIter.next();
            Integer nextIndex = current.childrenIndices[childIndex.asInt()];

            // If the `current` trie node doesn't contain a child with that next nibble
            // return `NotFound`...
            if (nextIndex == null) {
                return new ExistingNodeInnerResult.NotFound(closestAncestor);
            }

            // ... otherwise, continue with the traversal
            currentIndex = nextIndex;
        }
    }

    private sealed interface ExistingNodeInnerResult {

        /**
         * The node has been found
         *
         * @param nodeIndex       - index of the node
         * @param hasStorageValue - whether the node has a storage value or not
         */
        record Found(int nodeIndex, boolean hasStorageValue) implements ExistingNodeInnerResult {
        }

        /**
         * Closest ancestor that actually exists.
         * If null - we're inserting the root.
         *
         * @see ClosestAncestor
         */
        record NotFound(@Nullable ClosestAncestor closestAncestor) implements ExistingNodeInnerResult {

            /**
             * Closest ancestor that actually exists.
             *
             * @param index               the {@link TrieNodeIndex} of the closest ancestor node.
             * @param remainingKeyNibbles the remaining nibbles of the given key, starting from this closest ancestor.
             *                            <br>
             *                            Basically, a search has been invoked with full key <b>n<sub>1</sub>...n<sub>k</sub>n<sub>k+1</sub>...n<sub>l</sub></b>, where <b>n<sub>i</sub></b> are the separate nibbles.
             *                            The closest ancestor node has key <b>n<sub>1</sub>...n<sub>k</sub></b> (the deepest we could reach in the existing trie structure),
             *                            so the remaining nibbles <b>n<sub>k+1</sub>...n<sub>l</sub></b> are contained in {@code remainingKeyNibbles}.
             *                            <br>
             *                            This iterator is guaranteed to have at least one element.
             *                            NOTE: Currently unused.
             */
            record ClosestAncestor(Integer index, Nibbles remainingKeyNibbles) {
            }
        }

    }

    /**
     * A package-private method for inner work with the trie's nodes.
     * Accessible from all affiliate classes managing the inner state of the trie structure.
     *
     * @param nodeIndex the raw integer index of the node
     * @return the {@link TrieNode} at the given index
     * @throws InvalidSlabIndexException if the index is invalid
     */
    @NotNull
    TrieNode<T> getNodeAtIndexInner(int nodeIndex) {
        return this.nodes.get(nodeIndex);
    }

    /**
     * Returns the user data of the node by its index.
     * @param nodeIndex the index of the existing node
     * @return the user data of the node with the index provided
     */
    @Nullable
    public T getUserDataAtIndex(@NotNull TrieNodeIndex nodeIndex) {
        return this.getNodeAtIndexInner(nodeIndex.getValue()).userData;
    }

    /**
     * Returns a handle for the node at the given index.
     * @param nodeIndex the index of the existing node
     * @return a node handle for the node
     */
    @Nullable
    public NodeHandle<T> nodeHandleAtIndex(@NotNull TrieNodeIndex nodeIndex) {
        return nodeHandleAtIndexInner(nodeIndex.getValue());
    }

    @NotNull
    NodeHandle<T> nodeHandleAtIndexInner(int nodeIndex) {
        TrieNode<T> node = this.getNodeAtIndexInner(nodeIndex);
        return NodeHandle.<T>getConstructor(node.hasStorageValue).apply(this, nodeIndex);
    }

    /**
     * This method is a shortcut for {@link TrieStructure#nodeHandleAtIndex(TrieNodeIndex)}
     * followed by {@link NodeHandle#getFullKey()}
     *
     * @param nodeIndex index of a node
     * @return the key of the node at the given index
     */
    @NotNull
    public Nibbles nodeFullKeyAtIndex(TrieNodeIndex nodeIndex) {
        return nodeFullKeyAtIndexInner(nodeIndex.getValue());
    }

    @NotNull
    Nibbles nodeFullKeyAtIndexInner(int targetNodeIndex) {
        Queue<Integer> nodePath = this.nodePath(targetNodeIndex); // path without target itself
        nodePath.add(targetNodeIndex); // add target to the end

        Stream<Nibble> nibblesStream = nodePath
            .stream()
            .flatMap(n -> {
                TrieNode<T> node = this.getNodeAtIndexInner(n);

                Stream<Nibble> childIndex = Stream.ofNullable(
                    node.parent == null
                        ? null
                        : node.parent.childIndexWithinParent()
                );

                Nibbles partialKey = node.partialKey.copy();

                return Stream.concat(childIndex, partialKey.stream());
            });

        return nibblesStream.collect(NibblesCollector.toNibbles());
    }

    /**
     * @param targetNodeIndex index of the target node
     * @return the indices to traverse to reach {@code target} from root, not including {@code target} itself.
     *         So if {@code target} is root, returns an empty deque.
     * @throws NullPointerException if targetNodeIndex is not a valid index
     */
    Deque<Integer> nodePath(int targetNodeIndex) {
        Deque<Integer> path = new LinkedList<>();
        var current = this.getNodeAtIndexInner(targetNodeIndex).parent;

        while (current != null) {
            int nodeIndex = current.parentNodeIndex();
            path.addFirst(nodeIndex);
            current = this.getNodeAtIndexInner(nodeIndex).parent;
        }

        return path;
    }

    /**
     * Returns true if the structure of this trie is the same as the structure of {@code other}.
     * Everything is compared for equality except for the {@link TrieNode#userData}s.
     *
     * @implNote This method first compares the sizes of the two trie structures,
     *           and if they don't match, it early returns false.
     *           Otherwise, it performs the expensive operation of traversing both tries
     *           in order to identify a potential mismatch.
     */
    public <U> boolean structurallyEquals(TrieStructure<U> other) {
        if (this.nodes.size() != other.nodes.size()) {
            return false;
        }

        var thisIter = this.iteratorOrdered();
        var otherIter = other.iteratorOrdered();

        while (true) {
            if (!thisIter.hasNext() && !otherIter.hasNext()) {
                return true;
            }

            var thisNode = this.getNodeAtIndexInner(thisIter.next().getValue());
            var otherNode = other.getNodeAtIndexInner(otherIter.next().getValue());

            if (thisNode.hasStorageValue != otherNode.hasStorageValue) {
                return false;
            }


            // Check if parents match.
            // We want to return false in all cases except:
            //   - both parents are null;
            //   - both parents are not null and the two nodes' child indices within them are the same
            {
                var thisNodeParent = thisNode.parent;
                var otherNodeParent = otherNode.parent;

                boolean bothParentsNull = thisNodeParent == null || otherNodeParent == null;
                boolean bothParentsNotNullAndSameChildIndices =
                    thisNodeParent != null
                    && otherNodeParent != null
                    && thisNodeParent.childIndexWithinParent().equals(otherNodeParent.childIndexWithinParent());

                if (!(bothParentsNull || bothParentsNotNullAndSameChildIndices)) {
                    return false;
                }
            }

            if (!thisNode.partialKey.equals(otherNode.partialKey)) {
                return false;
            }
        }
    }
}

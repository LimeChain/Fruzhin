package com.limechain.trie.structure;

import com.limechain.runtime.version.StateVersion;
import com.limechain.trie.structure.nibble.Nibble;
import com.limechain.trie.structure.nibble.Nibbles;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.ArrayUtils;
import org.javatuples.Pair;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.IntStream;

public final class Vacant<T> extends Entry<T> {
    /**
     * How many children a node has.
     */
    private static final int TRIE_NODE_CHILDREN_COUNT = 16;

    /**
     * Full key of the node to insert.
     */
    private final Nibbles key;

    /**
     * The closest known ancestor. Will become the parent of any newly-inserted node at this vacant spot.
     */
    @Nullable
    private final Integer closestAncestorIndex;

    Vacant(TrieStructure<T> trieStructure, Nibbles key, @Nullable Integer closestAncestorIndex) {
        super(trieStructure);
        this.key = key;
        this.closestAncestorIndex = closestAncestorIndex;
    }

    @Nullable
    public TrieNodeIndex getClosestAncestorIndex() {
        return closestAncestorIndex != null ? new TrieNodeIndex(closestAncestorIndex) : null;
    }

    /**
     * Prepares the operation of creating the node in question.
     *
     * @return a {@link PrepareInsert} object containing all the data necessary to actually perform the insertion.
     * @implNote This method analyzes the trie to prepare for the operation, but doesn't actually perform any insertion.
     * To perform the insertion, use the returned {@link PrepareInsert}.
     */
    public PrepareInsert<T> prepareInsert() {
        // Retrieve what will be the parent node after we insert the new node,
        // not taking branching into account yet.
        // If not null, contains its index and number of nibbles in its key.
        Pair<Integer, Integer> futureParent = null;
        futureParentInit:
        {
            // If the trie is empty
            if (this.trieStructure.rootIndex == null) {
                // ... but we somehow found an ancestor...
                if (this.closestAncestorIndex != null) {
                    // UNREACHABLE STATE
                    throw new IllegalStateException("Unreachable state reached: ancestor found in an empty trie.");
                }

                // ... and no ancestor has been rightfully found, simply insert one node (will effectively be the root)
                // This is kind of a special case that we handle by returning early.
                return new PrepareInsert.One<>(
                        this.trieStructure,
                        null,
                        key.copy(),
                        new Integer[TRIE_NODE_CHILDREN_COUNT]
                );
            } else if (this.closestAncestorIndex != null) {
                int keyLen = this.trieStructure.nodeFullKeyAtIndexInner(closestAncestorIndex).size();
                assert this.key.size() > keyLen : "We shouldn't have reached the vacant spot yet!";
                futureParent = new Pair<>(this.closestAncestorIndex, keyLen);
            }
        }

        // Get the existing child of `futureParent` that points towards the newly-inserted node,
        // or a successful early-return if none.
        int existingNodeIndex;
        existingNodeIndexInit:
        {
            if (futureParent == null) {
                existingNodeIndex = this.trieStructure.rootIndex;
            } else {
                int futureParentIndex = futureParent.getValue0();
                int futureParentKeyLen = futureParent.getValue1();

                // that's the nibble "index" within parent's children
                Nibble newChildNibbleIndex = this.key.get(futureParentKeyLen);
                TrieNode<T> futureParentNode = this.trieStructure.getNodeAtIndexInner(futureParentIndex);

                Integer existingChildNodeIndex = futureParentNode.childrenIndices[newChildNibbleIndex.asInt()];

                if (existingChildNodeIndex == null) {
                    // There is an empty slot in `futureParentNode` for our new node.
                    //
                    //
                    //           `future_parent`
                    //                 +-+
                    //             +-> +-+  <---------+
                    //             |    ^ ^------+    |
                    //             |    +--+     |    |
                    //            +-+      |     |    |
                    //   New node +-+     +-+   +-+  +-+  0 or more existing children
                    //                    +-+   +-+  +-+
                    //
                    //

                    return new PrepareInsert.One<>(
                            this.trieStructure,
                            new TrieNode.Parent(futureParentIndex, newChildNibbleIndex),
                            this.key.drop(futureParentKeyLen + 1),
                            new Integer[TRIE_NODE_CHILDREN_COUNT]
                    );
                } else {
                    existingNodeIndex = existingChildNodeIndex;
                    assert futureParentIndex ==
                           this.trieStructure.getNodeAtIndexInner(existingNodeIndex).parent.parentNodeIndex()
                            : "Parent index mismatch with trie's internal indexing.";
                }
            }
        }

        // `existingNodeIndex` and the new node are known to either have the same parent and the
        // same child index, or to both have no parent. Now let's compare their partial key.
        Nibbles existingNodePartialKey = this.trieStructure.getNodeAtIndexInner(existingNodeIndex).partialKey;
        Nibbles newNodePartialKey = this.key.drop(futureParent == null ? 0 : futureParent.getValue1() + 1);

        assert !existingNodePartialKey.equals(newNodePartialKey)
                : "The remaining partial key cannot coincide with an existing node's partial key " +
                  "while inserting into a vacant spot.";
        assert !newNodePartialKey.startsWith(existingNodePartialKey)
                : "New node's partial key cannot begin with another existing node's partial key, " +
                  "because then that existing node would've been it's closest ancestor.";

        // If `existingNodePartialKey` starts with `newNodePartialKey`, then the new node
        // will be inserted in-between the parent and the existing node.
        if (existingNodePartialKey.startsWith(newNodePartialKey)) {
            // The new node is to be inserted in-between `futureParent` and
            // `existingNodeIndex`.
            //
            // If `futureParent` is not null:
            //
            //
            //                         +-+
            //        `futureParent`   +-+ <---------+
            //                          ^            |
            //                          |            +
            //                         +-+         (0 or more existing children)
            //               New node  +-+
            //                          ^
            //                          |
            //                         +-+
            //    `existingNodeIndex`  +-+
            //                          ^
            //                          |
            //                          +
            //                    (0 or more existing children)
            //
            //
            //
            // If `futureParent` is null:
            //
            //
            //            New node    +-+
            //    (becomes the root)  +-+
            //                         ^
            //                         |
            //   `existingNodeIndex`  +-+
            //     (current root)     +-+
            //                         ^
            //                         |
            //                         +
            //                   (0 or more existing children)
            //

            var newNodeChildren = new Integer[TRIE_NODE_CHILDREN_COUNT];
            var existingNodeNewChildIndex = existingNodePartialKey.get(newNodePartialKey.size());
            newNodeChildren[existingNodeNewChildIndex.asInt()] = existingNodeIndex;

            return new PrepareInsert.One<>(
                    this.trieStructure,
                    Optional.ofNullable(futureParent).map(fp ->
                            new TrieNode.Parent(fp.getValue0(), this.key.get(fp.getValue1()))
                    ).orElse(null),
                    newNodePartialKey.copy(),
                    newNodeChildren
            );
        }

        // If we reach here, we know that we will need to create a new branch node in addition to
        // the new storage node.
        //
        // If `futureParent` is not null:
        //
        //
        //                  `futureParent`
        //
        //                        +-+
        //                        +-+ <--------+  (0 or more existing children)
        //                         ^
        //                         |
        //       New branch node  +-+
        //                        +-+ <-------+
        //                         ^          |
        //                         |          |
        //                        +-+        +-+
        //   `existingNodeIndex`  +-+        +-+  New storage node
        //                         ^
        //                         |
        //
        //                 (0 or more existing children)
        //
        //
        //
        // If `futureParent` is null:
        //
        //
        //     New branch node    +-+
        //     (becomes root)     +-+ <-------+
        //                         ^          |
        //                         |          |
        //   `existingNodeIndex`  +-+        +-+
        //     (current root)     +-+        +-+  New storage node
        //                         ^
        //                         |
        //
        //                 (0 or more existing children)
        //
        //

        // Find the common ancestor between `newNodePartialKey` and `existingNodePartialKey`.
        int branchPartialKeyLen;
        branchPartialKeyLenInit:
        {
            assert !newNodePartialKey.equals(existingNodePartialKey)
                    :
                    "The partial key remaining for the node to be inserted can't match an existing node's partial key, " +
                    "since then this entry must've not been vacant.";

            // Since `newNodePartialKey` is different from `existingNodePartialKey`, we know
            // that `k1.next()` and `k2.next()` won't both be `None`.
            int len = (int) IntStream
                    .range(0, Math.min(newNodePartialKey.size(), existingNodePartialKey.size()))
                    .takeWhile(i -> newNodePartialKey.get(i).equals(existingNodePartialKey.get(i)))
                    .count();

            assert len < newNodePartialKey.size()
                    : "Common prefix (i.e. new branch node's partial key) " +
                      "length must be less than the new node's partial key";
            assert len < existingNodePartialKey.size()
                    : "Common prefix (i.e. new branch node's partial key) " +
                      "length must be less than the existing node's partial key";

            branchPartialKeyLen = len;
        }

        // Table of children for the new branch node, not including the new storage node.
        // It therefore contains only one entry: `existing_node_index`.
        Integer[] branchChildren = new Integer[TRIE_NODE_CHILDREN_COUNT];
        branchChildrenInit:
        {
            Nibble existingNodeNewChildIndex = existingNodePartialKey.get(branchPartialKeyLen);
            assert !existingNodeNewChildIndex.equals(newNodePartialKey.get(branchPartialKeyLen))
                    : "The paths must diverge here!";
            branchChildren[existingNodeNewChildIndex.asInt()] = existingNodeIndex;
        }

        // Success!
        return new PrepareInsert.Two<>(
                this.trieStructure,
                newNodePartialKey.get(branchPartialKeyLen),
                newNodePartialKey.drop(branchPartialKeyLen + 1),
                futureParent == null ? null
                        : new TrieNode.Parent(futureParent.getValue0(), this.key.get(futureParent.getValue1())),
                newNodePartialKey.take(branchPartialKeyLen),
                branchChildren
        );
    }

    /**
     * A class to hold all the data necessary to insert a new node,
     * i.e. this class contains the 'preparation to insert' a new node.
     * <br><br>
     * The trie hasn't been modified yet, and you can safely drop this object.
     *
     * @param <T> the UserData type (the generic parameter of the underlying {@code TrieStructure<T>})
     */
    @AllArgsConstructor
    public static abstract sealed class PrepareInsert<T> {
        protected TrieStructure<T> trieStructure;

        public StorageNodeHandle<T> insert(T storageUserData, StateVersion stateVersion) {
            return switch (this) {
                case One<T> one -> one.insert(storageUserData, stateVersion);
                // NOTE: If we decide it's needed, we can utilize `Two`'s capability to also insert user data at the
                //  branch node. Not needed for now.
                case Two<T> two -> two.insert(storageUserData, stateVersion);
            };
        }

        /**
         * One node will be inserted in the trie.
         */
        private static final class One<T> extends PrepareInsert<T> {
            /**
             * Value of {@link TrieNode#parent} for the newly-created node.
             * If null, we're setting the root of the trie to the new node when inserting
             * (see {@link One#insert(Object)} for more info on the insertion logic)
             */
            @Nullable
            private final TrieNode.Parent parent;

            /**
             * Value of {@link TrieNode#partialKey} for the newly-created node.
             */
            private final Nibbles partialKey;

            /**
             * Value of {@link TrieNode#childrenIndices} for the newly-created node.
             */
            private final Integer[] childrenIndices;

            private One(TrieStructure<T> trieStructure,
                        @Nullable TrieNode.Parent parent,
                        Nibbles partialKey,
                        Integer[] childrenIndices) {
                super(trieStructure);
                this.parent = parent;
                this.partialKey = partialKey;
                this.childrenIndices = childrenIndices;
            }

            /**
             * Insert the new storage node
             *
             * @param userData the userData held by the storage node
             * @return a StorageNodeHandle to the freshly inserted node
             */
            @Override
            public StorageNodeHandle<T> insert(T userData, StateVersion stateVersion) {
                int newNodePartialKeyLen = this.partialKey.size();
                int newNodeIndex = this.trieStructure.nodes.add(new TrieNode<>(
                        this.parent,
                        this.partialKey,
                        this.childrenIndices,
                        true,
                        userData,
                        stateVersion
                ));

                // Update the children nodes to point to their new parent.
                for (int childIndex = 0; childIndex < TRIE_NODE_CHILDREN_COUNT; ++childIndex) {
                    if (this.childrenIndices[childIndex] == null) {
                        continue;
                    }

                    TrieNode<T> childNode = this.trieStructure.getNodeAtIndexInner(this.childrenIndices[childIndex]);
                    Nibble childIndexNibble = Nibble.fromInt(childIndex);
                    childNode.parent = new TrieNode.Parent(newNodeIndex, childIndexNibble);
                    childNode.partialKey = childNode.partialKey.drop(newNodePartialKeyLen + 1);
                }

                // Update the parent to point to its new child.
                if (this.parent == null) {
                    this.trieStructure.rootIndex = newNodeIndex;
                } else {
                    TrieNode<T> parent = this.trieStructure.getNodeAtIndexInner(this.parent.parentNodeIndex());
                    parent.childrenIndices[this.parent.childIndexWithinParent().asInt()] = newNodeIndex;
                }

                // Success!
                return new StorageNodeHandle<>(this.trieStructure, newNodeIndex);
            }
        }

        /**
         * Two nodes will be inserted in the trie.
         * One intermediate branch node, which is structurally necessary,
         * and one to hold the actual storage value, which is the target at the end of the full key provided.
         */
        private static final class Two<T> extends PrepareInsert<T> {
            /**
             * Value of the child index in {@link TrieNode#parent} for the newly-created storage node.
             */
            private final Nibble storageChildIndex;

            /**
             * Value of {@link TrieNode#partialKey} for the newly-created storage node.
             */
            private final Nibbles storagePartialKey;

            /**
             * Value of {@link TrieNode#parent} for the newly-created branch node.
             * If null, we're also setting the root of the trie to the new branch node.
             */
            @Nullable
            private final TrieNode.Parent branchParent;

            /**
             * Value of {@link TrieNode#partialKey} for the newly-created branch node.
             */
            private final Nibbles branchPartialKey;

            /**
             * Value of {@link TrieNode#childrenIndices} for the newly-created branch node.
             * Does not include the entry that must be filled with the new storage node.
             */
            private final Integer[] branchChildrenIndices;

            private Two(TrieStructure<T> trieStructure,
                        Nibble storageChildIndex,
                        Nibbles storagePartialKey,
                        @Nullable TrieNode.Parent branchParent,
                        Nibbles branchPartialKey,
                        Integer[] branchChildrenIndices) {
                super(trieStructure);
                this.storageChildIndex = storageChildIndex;
                this.storagePartialKey = storagePartialKey;
                this.branchParent = branchParent;
                this.branchPartialKey = branchPartialKey;
                this.branchChildrenIndices = branchChildrenIndices;
            }

            /**
             * Insert the new storage node (and the intermediate branch node needed for this case)
             * Branch node's user data defaults to null.
             *
             * @param storageUserData the userData held by the storage node
             * @return a StorageNodeHandle to the freshly inserted storage node
             */
            @Override
            public StorageNodeHandle<T> insert(T storageUserData, StateVersion stateVersion) {
                return this.insert(storageUserData, null, stateVersion);
            }

            /**
             * Insert the new storage node (and the intermediate branch node needed for this case)
             *
             * @param storageUserData the userData held by the storage node
             * @param branchUserData  the userData held by the branch node
             * @return a StorageNodeHandle to the freshly inserted storage node
             */
            @SuppressWarnings("unused")
            public StorageNodeHandle<T> insert(T storageUserData, T branchUserData, StateVersion stateVersion) {
                int newBranchNodePartialKeyLen = this.branchPartialKey.size();

                assert 1 == Arrays.stream(this.branchChildrenIndices).filter(Objects::nonNull).count()
                        : "The branch node we're about to insert must have exactly one child " +
                          "(the node that previously existed before this insertion)";

                // Insert the intermediate branch node
                int newBranchNodeIndex = this.trieStructure.nodes.add(new TrieNode<>(
                        this.branchParent,
                        this.branchPartialKey,
                        // NOTE: Crucial mutability moment, this has to be a copy of the array.
                        //  I'll never get those three hours of my life back :)
                        //  Manage your ownership, kids.
                        ArrayUtils.clone(this.branchChildrenIndices),
                        false,
                        branchUserData,
                        stateVersion
                ));

                // Insert the actual storage node
                int newStorageNodeIndex = this.trieStructure.nodes.add(new TrieNode<>(
                        new TrieNode.Parent(newBranchNodeIndex, this.storageChildIndex),
                        this.storagePartialKey,
                        new Integer[TRIE_NODE_CHILDREN_COUNT],
                        true,
                        storageUserData,
                        stateVersion
                ));

                // Set the freshly obtained storage node's index in the child array of the branch node
                this.trieStructure
                        .getNodeAtIndexInner(newBranchNodeIndex)
                        .childrenIndices[this.storageChildIndex.asInt()] = newStorageNodeIndex;

                // Update the branch node's children to point to their new parent
                for (int childIndex = 0; childIndex < TRIE_NODE_CHILDREN_COUNT; ++childIndex) {
                    if (this.branchChildrenIndices[childIndex] == null) {
                        continue;
                    }

                    TrieNode<T> childNode =
                            this.trieStructure.getNodeAtIndexInner(this.branchChildrenIndices[childIndex]);
                    Nibble childIndexNibble = Nibble.fromInt(childIndex);
                    childNode.parent = new TrieNode.Parent(newBranchNodeIndex, childIndexNibble);
                    childNode.partialKey = childNode.partialKey.drop(newBranchNodePartialKeyLen + 1);
                }

                // Update the branch node's parent to point to its new child.
                if (this.branchParent == null) {
                    this.trieStructure.rootIndex = newBranchNodeIndex;
                } else {
                    TrieNode<T> parent = this.trieStructure.getNodeAtIndexInner(this.branchParent.parentNodeIndex());
                    parent.childrenIndices[this.branchParent.childIndexWithinParent().asInt()] = newBranchNodeIndex;
                }

                // Success!
                return new StorageNodeHandle<>(this.trieStructure, newStorageNodeIndex);
            }
        }
    }
}

package com.limechain.trie.structure;

import com.limechain.trie.structure.nibble.Nibble;
import com.limechain.trie.structure.nibble.Nibbles;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents a node in the trie structure. Mutable by design.
 * Fields are intentionally package-protected, since the separations of constructing and modifying trie nodes
 * is spread among a couple of classes within this package.
 */
@AllArgsConstructor
class TrieNode<T> {
    /**
     * Index of the parent node within {@link TrieStructure#nodes} plus this node's child index within the parent.
     * Null if this is the root.
     */
    @Nullable
    Parent parent;

    /**
     * Partial key of the node
     */
    @NotNull
    Nibbles partialKey;

    /**
     * A non-null fixed-size (16) array of nullable Integers representing child node indices (if null, no child)
     */
    @NotNull
    Integer[] childrenIndices;

    /**
     * Whether the node has a storage value or not
     */
    boolean hasStorageValue;

    /**
     * Optional user data attached to this node
     * (with no particular semantics, but could be used to represent the actual storage value)
     */
    @Nullable
    T userData;

    /**
     * @return the lexicographically first ('0' to 'f') child node's index, Optional.empty() if no children.
     */
    Optional<Integer> firstChild() {
        return Arrays.stream(this.childrenIndices)
            .filter(Objects::nonNull)
            .findFirst();
    }

    /**
     * Contains information about the parent's index and this node's child index within the parent.
     * @param parentNodeIndex the raw {@link TrieNodeIndex} of the parent node
     * @param childIndexWithinParent the child index of this {@link TrieNode} within the parent.
     *                               Child index means a nibble, i.e. in the range [0, 15].
     */
    record Parent(int parentNodeIndex, @NotNull Nibble childIndexWithinParent) {}
}

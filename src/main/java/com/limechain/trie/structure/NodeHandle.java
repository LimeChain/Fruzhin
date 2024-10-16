package com.limechain.trie.structure;

import com.limechain.trie.structure.nibble.Nibble;
import com.limechain.trie.structure.nibble.Nibbles;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.BiFunction;

/**
 * Indicates that the given {@link Entry} is an actual node in the trie.
 * <br>
 * <br>
 * This {@link NodeHandle} could be either one of:
 * <ul>
 *     <li>a {@link StorageNodeHandle}, meaning the node has a storage value (could be either leaf or branch node)</li>
 *     <li>a {@link BranchNodeHandle}, meaning the node is a purely structural branch node with no storage value.</li>
 * </ul>
 */
public abstract sealed class NodeHandle<T> extends Entry<T> permits StorageNodeHandle, BranchNodeHandle {
    protected final int rawNodeIndex;

    protected NodeHandle(TrieStructure<T> trieStructure, int rawNodeIndex) {
        super(trieStructure);
        this.rawNodeIndex = rawNodeIndex;
    }

    //NOTE:
    // Later on, we might switch from optionals to nullable return types
    // Depends on whether Optionals will be more helpful or more annoying
    /**
     * Gets the node handle of this node's child at the given index.
     * @param index The Nibble index (a.k.a. child index) within this node's children.
     * @return the optional node handle of this child node, empty is no child exists at the given index
     */
    public Optional<NodeHandle<T>> getChild(Nibble index) {
        TrieNode<T> node = this.trieStructure.getNodeAtIndexInner(this.rawNodeIndex);
        return Optional
            .ofNullable(node.childrenIndices[index.asInt()])
            .map(this.trieStructure::nodeHandleAtIndexInner);
    }

    /**
     * @return the partial key of the node.
     */
    @NotNull
    public Nibbles getPartialKey() {
        return this.trieStructure.getNodeAtIndexInner(this.rawNodeIndex).partialKey;
    }

    /**
     * @return the full key of the node.
     */
    @NotNull
    public Nibbles getFullKey() {
        return this.trieStructure.nodeFullKeyAtIndexInner(this.rawNodeIndex);
    }

    /**
     * @return  the {@link TrieNode#userData} of the underlying node this handle points to,
     *          null if the node has no user data.
     */
    @Nullable
    public T getUserData() {
        return this.trieStructure.getNodeAtIndexInner(this.rawNodeIndex).userData;
    }

    /**
     * Sets the {@link TrieNode#userData} of the underlying node, pointed to by this handle.
     * @param userData the new user data to be set
     */
    public void setUserData(@Nullable T userData) {
        this.trieStructure.getNodeAtIndexInner(this.rawNodeIndex).userData = userData;
    }

    /**
     * Returns a handle to the parent node of this node. Null if this is the root.
     */
    @Nullable
    public NodeHandle<T> getParent() {
        var parentIndex = this.trieStructure.nodes.get(this.rawNodeIndex).parent;

        if (parentIndex == null) {
            return null;
        }

        return this.trieStructure.nodeHandleAtIndexInner(parentIndex.parentNodeIndex());
    }

    /**
     * @return true if this node is the root node of the trie; false otherwise.
     */
    public boolean isRootNode() {
        return Integer.valueOf(this.rawNodeIndex).equals(this.trieStructure.rootIndex);
    }

    /**
     * @return the TrieNodeIndex of the node for that handle
     */
    public TrieNodeIndex getNodeIndex() {
        return new TrieNodeIndex(this.rawNodeIndex);
    }

    /**
     * @return true if the node has a storage value
     */
    public abstract boolean hasStorageValue();

    /**
     * Return a NodeHandle constructor depending on whether we want
     * a {@link StorageNodeHandle} (if hasStorageValue is true) or a {@link BranchNodeHandle} (if false).
     * Purely a convenience method to reduce boilerplate outside.
     * @param hasStorageValue whether the node has a storage value
     * @return the corresponding node handle constructor
     */
    static <T> BiFunction<TrieStructure<T>, Integer, NodeHandle<T>> getConstructor(boolean hasStorageValue) {
        return hasStorageValue
            ? StorageNodeHandle::new
            : BranchNodeHandle::new;
    }

    public int getStateVersion() {
        return this.trieStructure.getNodeAtIndexInner(this.rawNodeIndex).stateVersion.asInt();
    }
}

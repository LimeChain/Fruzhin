package com.limechain.trie.structure;

/**
 * Has the semantics of a "branch node with no storage value",
 * i.e. in the purely "structural necessity branch node" sense.
 */
public final class BranchNodeHandle<T> extends NodeHandle<T> {
    private boolean consumed;

    BranchNodeHandle(TrieStructure<T> trieStructure, int nodeIndex) {
        super(trieStructure, nodeIndex);
        consumed = false;
    }

    @Override
    public boolean hasStorageValue() {
        return false;
    }

    /**
     * Marks this node as a storage node, thus converting {@code this} into a {@link StorageNodeHandle}.
     * <br>
     * This doesn't change the trie structure.
     * @return the newly created {@link StorageNodeHandle}
     * @implNote After calling this method, {@code this} instance is no longer valid,
     *           since the underlying node has been converted to a storage node,
     *           thus it's no longer a branch node.
     *           Any further usage of this {@link BranchNodeHandle} would be invalid.
     * @throws IllegalStateException if this branch node handle has already been consumed,
     *                               i.e. the referenced node has been converted to a storage node.
     */
    public StorageNodeHandle<T> convertToStorageNode() {
        if (this.consumed) {
            throw new IllegalStateException(
                "Branch node has already been converted to a storage node, so this handle is invalid.");
        }

        TrieNode<T> node = this.trieStructure.getNodeAtIndexInner(this.rawNodeIndex);
        assert !node.hasStorageValue : "Branch node cannot have a storage value.";
        node.hasStorageValue = true;
        consumed = true;

        return new StorageNodeHandle<>(this.trieStructure, this.rawNodeIndex);
    }

}

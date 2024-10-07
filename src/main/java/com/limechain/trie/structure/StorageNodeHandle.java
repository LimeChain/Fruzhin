package com.limechain.trie.structure;

/**
 * Has the semantics of a "node with a storage value",
 * i.e. might be a branch node, might be a leaf node, doesn't matter.
 */
public final class StorageNodeHandle<T> extends NodeHandle<T> {
    boolean consumed;
    StorageNodeHandle(TrieStructure<T> trieStructure, int nodeIndex) {
        super(trieStructure, nodeIndex);
        consumed = false;
    }

    @Override
    public boolean hasStorageValue() {
        return true;
    }

    public BranchNodeHandle<T> convertToBranchNode() {
        if (this.consumed) {
            throw new IllegalStateException(
                    "Storage node has already been converted to a branch node, so this handle is invalid.");
        }

        TrieNode<T> node = this.trieStructure.getNodeAtIndexInner(this.rawNodeIndex);
        node.hasStorageValue = false;

        return new BranchNodeHandle<>(this.trieStructure, this.rawNodeIndex);

    }
}

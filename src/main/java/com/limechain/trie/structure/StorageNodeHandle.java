package com.limechain.trie.structure;

/**
 * Has the semantics of a "node with a storage value",
 * i.e. might be a branch node, might be a leaf node, doesn't matter.
 */
public final class StorageNodeHandle<T> extends NodeHandle<T> {
    StorageNodeHandle(TrieStructure<T> trieStructure, int nodeIndex) {
        super(trieStructure, nodeIndex);
    }

    @Override
    public boolean hasStorageValue() {
        return true;
    }

    public boolean clearStorageValue() {
        return trieStructure.clearNodeValue(rawNodeIndex);
    }

    //convert to branchNode
    public BranchNodeHandle<T> convertToBranchNode() {
        TrieNode<T> node = this.trieStructure.getNodeAtIndexInner(this.rawNodeIndex);
        assert node.hasStorageValue : "Storage node cannot be converted to a branch node.";
        node.hasStorageValue = false;
        return new BranchNodeHandle<>(this.trieStructure, this.rawNodeIndex);
    }
}

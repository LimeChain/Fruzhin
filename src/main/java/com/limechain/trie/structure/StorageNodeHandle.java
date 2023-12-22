package com.limechain.trie.structure;

/**
 * Has the semantics of a "node with a storage value",
 * i.e. might be a branch node, might be a leaf node, doesn't matter.
 */
public final class StorageNodeHandle<T> extends NodeHandle<T> {
    StorageNodeHandle(TrieStructure<T> trieStructure, int nodeIndex) {
        super(trieStructure, nodeIndex);
    }
}

package com.limechain.trie.structure.node.handle;

import com.limechain.trie.structure.TrieStructure;

public final class StorageNodeHandle<T> extends NodeHandle<T> {
    public StorageNodeHandle(TrieStructure<T> trieStructure, int nodeIndex) {
        super(trieStructure, nodeIndex);
    }
}

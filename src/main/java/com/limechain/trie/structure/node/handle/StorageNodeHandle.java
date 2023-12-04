package com.limechain.trie.structure.node.handle;

import com.limechain.trie.structure.TrieStructure;
import com.limechain.trie.structure.node.TrieNodeIndex;

public final class StorageNodeHandle<T> extends NodeHandle<T> {
    public StorageNodeHandle(TrieStructure<T> trieStructure, TrieNodeIndex nodeIndex) {
        super(trieStructure, nodeIndex);
    }
}

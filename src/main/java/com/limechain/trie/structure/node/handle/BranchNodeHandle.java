package com.limechain.trie.structure.node.handle;

import com.limechain.trie.structure.TrieStructure;
import com.limechain.trie.structure.node.TrieNodeIndex;

public final class BranchNodeHandle<T> extends NodeHandle<T> {
    public BranchNodeHandle(TrieStructure<T> trieStructure, TrieNodeIndex nodeIndex) {
        super(trieStructure, nodeIndex);
    }
}

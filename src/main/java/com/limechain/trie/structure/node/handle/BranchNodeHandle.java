package com.limechain.trie.structure.node.handle;

import com.limechain.trie.structure.TrieStructure;

public final class BranchNodeHandle<T> extends NodeHandle<T> {
    public BranchNodeHandle(TrieStructure<T> trieStructure, int nodeIndex) {
        super(trieStructure, nodeIndex);
    }
}

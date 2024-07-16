package com.limechain.trie;

import com.limechain.storage.trie.TrieStorage;
import com.limechain.trie.structure.TrieStructure;
import com.limechain.trie.structure.database.NodeData;

/**
 * BlockTrieAccessor provides access to the trie structure of a specific block.
 * It extends TrieAccessor and inherits its functionalities for key-value storage and retrieval.
 */
public final class BlockTrieAccessor extends MemoryTrieAccessor {
    public BlockTrieAccessor(TrieStorage trieStorage, byte[] stateRoot) {
        super(trieStorage, stateRoot);
    }

    public BlockTrieAccessor(TrieStorage trieStorage, byte[] stateRoot, TrieStructure<NodeData> trieStructure) {
        super(trieStorage, stateRoot, trieStructure);
    }
}

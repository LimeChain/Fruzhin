package com.limechain.runtime.research.hybrid.trieaccessor;

import com.limechain.storage.trie.TrieStorage;
import com.limechain.trie.structure.nibble.Nibbles;
import lombok.Getter;

/**
 * ChildTrieAccessor provides access to a child trie within a parent trie structure.
 * It extends TrieAccessor and inherits its functionalities for key-value storage and retrieval.
 */
public class ChildTrieAccessor extends TrieAccessor {

    private final TrieAccessor parentTrie;
    @Getter
    private final Nibbles childTrieKey;

    public ChildTrieAccessor(TrieStorage trieStorage, TrieAccessor parentTrie, Nibbles trieKey, byte[] merkleRoot) {
        super(trieStorage, merkleRoot);
        this.parentTrie = parentTrie;
        this.childTrieKey = trieKey;
    }

    @Override
    public void persistUpdates() {
        super.persistUpdates();
        parentTrie.save(childTrieKey, mainTrieRoot);
    }

}

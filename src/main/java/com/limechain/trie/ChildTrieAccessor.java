package com.limechain.trie;

import com.limechain.storage.trie.TrieStorage;
import com.limechain.trie.structure.nibble.Nibbles;
import lombok.Getter;

/**
 * ChildTrieAccessor provides access to a child trie within a parent trie structure.
 * It extends TrieAccessor and inherits its functionalities for key-value storage and retrieval.
 */
public final class ChildTrieAccessor extends MemoryTrieAccessor {

    private final MemoryTrieAccessor parentTrie;
    @Getter
    private final Nibbles childTrieKey;

    public ChildTrieAccessor(TrieStorage trieStorage, MemoryTrieAccessor parentTrie, Nibbles trieKey, byte[] merkleRoot) {
        super(trieStorage, merkleRoot);
        this.parentTrie = parentTrie;
        this.childTrieKey = trieKey;
    }

    @Override
    public void persistChanges() {
        super.persistChanges();
        parentTrie.upsertNode(childTrieKey, mainTrieRoot);
    }

}

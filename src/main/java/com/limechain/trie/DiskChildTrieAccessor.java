package com.limechain.trie;

import com.limechain.storage.trie.TrieStorage;
import com.limechain.trie.structure.nibble.Nibbles;
import lombok.Getter;

public final class DiskChildTrieAccessor extends DiskTrieAccessor {

    private final DiskTrieAccessor parentTrie;
    @Getter
    private final Nibbles childTrieKey;

    public DiskChildTrieAccessor(TrieStorage trieStorage,
                                 DiskTrieAccessor parentTrie,
                                 Nibbles trieKey,
                                 byte[] merkleRoot) {
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
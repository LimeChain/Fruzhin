package com.limechain.trie;

import com.limechain.trie.structure.nibble.Nibbles;
import lombok.Getter;

public class ChildTrieAccessor extends TrieAccessor {

    private final TrieAccessor parentTrie;
    @Getter
    private final Nibbles childTrieKey;

    public ChildTrieAccessor(TrieAccessor parentTrie, Nibbles trieKey, byte[] merkleRoot) {
        super(merkleRoot);
        this.parentTrie = parentTrie;
        this.childTrieKey = trieKey;
    }

    @Override
    public void persistAll() {
        super.persistAll();
        parentTrie.save(childTrieKey, lastRoot);
    }

}

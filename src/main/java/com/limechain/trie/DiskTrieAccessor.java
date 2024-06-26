package com.limechain.trie;

import com.limechain.storage.DeleteByPrefixResult;
import com.limechain.trie.structure.nibble.Nibbles;

import java.util.Optional;

public final class DiskTrieAccessor implements TrieAccessor {

    @Override
    public void upsertNode(Nibbles key, byte[] value) {

    }

    @Override
    public void deleteNode(Nibbles key) {

    }

    @Override
    public Optional<byte[]> findStorageValue(Nibbles key) {
        return Optional.empty();
    }

    @Override
    public DeleteByPrefixResult deleteMultipleNodesByPrefix(Nibbles prefix, Long limit) {
        return null;
    }

    @Override
    public Optional<Nibbles> getNextKey(Nibbles key) {
        return Optional.empty();
    }

    @Override
    public void persistChanges() {

    }
}

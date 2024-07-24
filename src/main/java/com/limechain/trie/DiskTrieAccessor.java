package com.limechain.trie;

import com.limechain.storage.DeleteByPrefixResult;
import com.limechain.storage.trie.TrieStorage;
import com.limechain.trie.structure.nibble.Nibbles;

import java.util.Optional;

public sealed class DiskTrieAccessor extends TrieAccessor permits DiskChildTrieAccessor {

    private final DiskTrieService diskTrieService;

    public DiskTrieAccessor(TrieStorage trieStorage, byte[] mainTrieRoot) {
        super(trieStorage, mainTrieRoot);

        this.diskTrieService = new DiskTrieService(trieStorage);
    }

    @Override
    public void upsertNode(Nibbles key, byte[] value) {
//        diskTrieService.upsertNode(mainTrieRoot, key, value);
    }

    @Override
    public void deleteNode(Nibbles key) {
//        trieChanges.diffInsertErase(key, null);
    }

    @Override
    public Optional<byte[]> findStorageValue(Nibbles key) {
//        // TODO 437 this is wrong. If we have a deletion cache will return empty optional.
//        Optional<byte[]> result = trieChanges.trieDiffGet(key);
//        if (result.isEmpty()) {
//            result = trieStorage.getByKeyFromMerkle(mainTrieRoot, key)
//                .flatMap(nodeDta -> Optional.ofNullable(nodeDta.getValue()));
//        }
//
//        return result;\

        return null;
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
        super.persistChanges();
//        trieChanges = TrieChanges.empty();
    }

    @Override
    public DiskChildTrieAccessor getChildTrie(Nibbles key) {
        return (DiskChildTrieAccessor) super.getChildTrie(key);
    }

    @Override
    public DiskChildTrieAccessor createChildTrie(Nibbles trieKey, byte[] merkleRoot) {
        return new DiskChildTrieAccessor(trieStorage, this, trieKey, merkleRoot);
    }
}

package com.limechain.trie;

import com.limechain.storage.DeleteByPrefixResult;
import com.limechain.storage.trie.TrieStorage;
import com.limechain.trie.cache.TrieChanges;
import com.limechain.trie.structure.database.NodeData;
import com.limechain.trie.structure.nibble.Nibbles;

import java.util.Optional;

public sealed class DiskTrieAccessor extends TrieAccessor permits DiskChildTrieAccessor {

    private TrieChanges<NodeData> trieChanges;

    public DiskTrieAccessor(TrieStorage trieStorage, byte[] mainTrieRoot) {
        super(trieStorage, mainTrieRoot);

        this.trieChanges = TrieChanges.empty();
    }

    @Override
    public void upsertNode(Nibbles key, byte[] value) {
        NodeData nodeData = new NodeData(value);
        trieChanges.diffInsert(key, value, nodeData);
    }

    @Override
    public void deleteNode(Nibbles key) {
        trieChanges.diffInsertErase(key, null);
    }

    @Override
    public Optional<byte[]> findStorageValue(Nibbles key) {
        Optional<byte[]> result = trieChanges.trieDiffGet(key);
        if (result.isEmpty()) {
            result = trieStorage.getByKeyFromMerkle(mainTrieRoot, key)
                .flatMap(nodeDta -> Optional.ofNullable(nodeDta.getValue()));
        }

        return result;
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
        trieChanges = TrieChanges.empty();
    }

    @Override
    public DiskChildTrieAccessor getChildTrie(Nibbles key) {
        Nibbles trieKey = Nibbles.fromBytes(":child_storage:default:".getBytes()).addAll(key);
        byte[] merkleRoot = findStorageValue(trieKey).orElse(null);

        return (DiskChildTrieAccessor) loadedChildTries.computeIfAbsent(
            trieKey, k -> new DiskChildTrieAccessor(trieStorage, this, trieKey, merkleRoot));
    }
}

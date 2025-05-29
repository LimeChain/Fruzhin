package com.limechain.trie;

import com.limechain.state.StateManager;
import com.limechain.storage.trie.TrieStorage;
import io.emeraldpay.polkaj.types.Hash256;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Log
@Component
@RequiredArgsConstructor
public class TrieAccessorStorage {

    private final TrieStorage trieStorage;
    private final StateManager stateManager;
    private final Map<Hash256, DiskTrieAccessor> diskAccessors = new ConcurrentHashMap<>();

    public DiskTrieAccessor init(Hash256 hash, Hash256 stateRoot) {
        return diskAccessors.computeIfAbsent(hash,
                _ -> new DiskTrieAccessor(trieStorage, stateRoot.getBytes()));
    }

    public DiskTrieAccessor appendStorage(Hash256 hash, TrieAccessor toCopy) {
        return diskAccessors.computeIfAbsent(hash, _ -> new DiskTrieAccessor(toCopy));
    }

    public DiskTrieAccessor get(Hash256 hash) {
        return diskAccessors.get(hash);
    }

    public void prune(Hash256 finalizedHash) {
        diskAccessors.keySet().removeIf(key ->
                !stateManager.getBlockState().isDescendantOf(finalizedHash, key));
    }
}

package com.limechain.storage.offchain;

import com.limechain.storage.KVRepository;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class OffchainStore {
    private static final String OFFCHAIN_PREFIX = "offchain_";

    private final KVRepository<String, Object> repository;
    private final String prefix;

    public OffchainStore(KVRepository<String, Object> repository, StorageKind storageKind) {
        this.repository = repository;
        this.prefix = OFFCHAIN_PREFIX.concat(storageKind.getPrefix());
    }

    public synchronized void set(String key, byte[] value) {
        repository.save(prefixedKey(key), value);
    }

    public synchronized byte[] get(String key) {
        return (byte[]) repository.find(prefixedKey(key)).orElse(null);
    }

    public synchronized void remove(String key) {
        repository.delete(prefixedKey(key));
    }

    public synchronized boolean compareAndSet(String key, byte[] oldValue, byte[] newValue) {
        String prefixedKey = prefixedKey(key);
        byte[] currentValue = (byte[]) repository.find(prefixedKey).orElse(null);

        if (currentValue != oldValue) {
            return false;
        }

        repository.save(prefixedKey, newValue);
        return true;
    }

    private String prefixedKey(String key) {
        return prefix.concat(key);
    }
}

package com.limechain.storage.offchain;

import com.limechain.storage.KVRepository;
import lombok.AllArgsConstructor;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static java.nio.charset.StandardCharsets.US_ASCII;

/**
 * OffchainStore provides methods for storing, retrieving, and manipulating offchain data.
 */
@AllArgsConstructor
public class OffchainStore implements BasicStorage {
    private static final String OFFCHAIN_PREFIX = "offchain_";

    private final KVRepository<String, Object> repository;
    private final String prefix;

    public OffchainStore(KVRepository<String, Object> repository, StorageKind storageKind) {
        this.repository = repository;
        this.prefix = OFFCHAIN_PREFIX.concat(storageKind.getPrefix());
    }

    public synchronized void set(byte[] key, byte[] value) {
        repository.save(prefixedKey(key), value);
    }

    public synchronized void set(String key, byte[] value) {
        this.set(key.getBytes(US_ASCII), value);
    }

    public synchronized byte[] get(byte[] key) {
        return (byte[]) repository.find(prefixedKey(key)).orElse(null);
    }

    public synchronized byte[] get(String key) {
        return this.get(key.getBytes(US_ASCII));
    }

    public synchronized void remove(byte[] key) {
        repository.delete(prefixedKey(key));
    }

    public synchronized void remove(String key) {
        this.remove(key.getBytes(US_ASCII));
    }

    public synchronized boolean compareAndSet(String key, byte[] oldValue, byte[] newValue) {
        return this.compareAndSet(key.getBytes(US_ASCII), oldValue, newValue);
    }

    private String prefixedKey(byte[] key) {
        return prefix.concat(new String(key));
    }
}

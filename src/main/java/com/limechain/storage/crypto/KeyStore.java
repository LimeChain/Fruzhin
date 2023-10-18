package com.limechain.storage.crypto;

import com.limechain.storage.KVRepository;
import lombok.extern.java.Log;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
@Log
public class KeyStore {

    private final KVRepository<String, Object> repository;

    public KeyStore(KVRepository<String, Object> repository) {
        this.repository = repository;
    }

    public void put(KeyType keyType, byte[] publicKey, byte[] privateKey) {
        repository.save(getKey(keyType, publicKey), privateKey);
    }

    public byte[] get(KeyType keyType, byte[] publicKey) {
        return (byte[]) repository.find(getKey(keyType, publicKey)).orElse(null);
    }

    public List<byte[]> getPublicKeysByKeyType(KeyType keyType) {
        return repository
                .findKeysByPrefix(new String(keyType.getBytes()), 90000)
                .stream()
                .map(this::removeKeyTypeFromKey)
                .toList();
    }

    private byte[] removeKeyTypeFromKey(byte[] key) {
        return Arrays.copyOfRange(key, 4, key.length);
    }

    private String getKey(KeyType keyType, byte[] key) {
        return new String(keyType.getBytes()).concat(new String(key));
    }
}

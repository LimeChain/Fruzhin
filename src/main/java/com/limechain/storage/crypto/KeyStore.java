package com.limechain.storage.crypto;

import com.limechain.storage.KVRepository;
import lombok.extern.java.Log;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

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
        return (byte[]) repository.find(getKey(keyType, publicKey)).get();
    }

    public List<byte[]> getPublicKeysByKeyType(KeyType keyType) {
        return repository
                .findKeysByPrefix(getKey(keyType, "".getBytes()), 90000)
                .stream()
                .map(this::removeKeyTypeFromKey)
                .collect(Collectors.toList());
    }

    private byte[] removeKeyTypeFromKey(String key) {
        return key.substring(4).getBytes();
    }

    private String getKey(KeyType keyType, byte[] key) {
        return new String(keyType.getBytes()).concat(new String(key));
    }
}

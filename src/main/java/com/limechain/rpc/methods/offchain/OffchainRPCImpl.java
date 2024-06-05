package com.limechain.rpc.methods.offchain;

import com.limechain.storage.KVRepository;
import com.limechain.storage.offchain.OffchainStore;
import com.limechain.storage.offchain.StorageKind;
import com.limechain.utils.StringUtils;
import org.springframework.stereotype.Service;

@Service
public class OffchainRPCImpl {

    private final OffchainStore persistentStorage;
    private final OffchainStore localStorage;

    public OffchainRPCImpl(final KVRepository<String, Object> db) {
        persistentStorage = new OffchainStore(db, StorageKind.PERSISTENT);
        localStorage = new OffchainStore(db, StorageKind.LOCAL);
    }

    /**
     * Sets a value in the specified offchain storage.
     * The key is expected to be in hexadecimal format and is converted internally. The value is stored as bytes.
     *
     * @param storageKind The type of storage (PERSISTENT or LOCAL) where the value is to be stored.
     * @param key The key under which the value is stored, provided in hexadecimal format.
     * @param value The value to be stored, provided in hexadecimal format.
     */
    public void offchainLocalStorageSet(StorageKind storageKind, String key, String value) {
        OffchainStore offchainStore = storageByKind(storageKind);
        offchainStore.set(StringUtils.hexToBytes(key), StringUtils.hexToBytes(value));
    }

    /**
     * Retrieves a value from the specified offchain storage.
     * The key is expected to be in hexadecimal format and is converted internally to retrieve the corresponding bytes.
     *
     * @param storageKind The type of storage (PERSISTENT or LOCAL) from which to retrieve the value.
     * @param key The key whose value is to be retrieved, provided in hexadecimal format.
     * @return The value associated with the key in hexadecimal format, prefixed with "0x", or null if the key does not exist.
     */
    public String offchainLocalStorageGet(StorageKind storageKind, String key) {
        OffchainStore offchainStore = storageByKind(storageKind);
        byte[] bytes = offchainStore.get(StringUtils.hexToBytes(key));
        if (bytes == null) {
            return null;
        }
        return  StringUtils.toHexWithPrefix(bytes);
    }

    private OffchainStore storageByKind(StorageKind kind) {
        return switch (kind) {
            case PERSISTENT -> persistentStorage;
            case LOCAL -> localStorage;
        };
    }
}

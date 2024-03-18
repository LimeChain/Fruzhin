package com.limechain.rpc.methods.offchain;

import com.limechain.storage.KVRepository;
import com.limechain.storage.offchain.OffchainStore;
import com.limechain.storage.offchain.StorageKind;
import com.limechain.utils.StringUtils;
import org.apache.tomcat.util.buf.HexUtils;
import org.springframework.stereotype.Service;

@Service
public class OffchainRPCImpl {

    private static final String HEX_PREFIX = "0x";
    private final OffchainStore persistentStorage;
    private final OffchainStore localStorage;

    public OffchainRPCImpl(final KVRepository<String, Object> db) {
        persistentStorage = new OffchainStore(db, StorageKind.PERSISTENT);
        localStorage = new OffchainStore(db, StorageKind.LOCAL);
    }

    public void offchainLocalStorageSet(StorageKind storageKind, String key, String value) {
        OffchainStore offchainStore = storageByKind(storageKind);
        offchainStore.set(new String(StringUtils.hexToBytes(key)), StringUtils.hexToBytes(value));
    }

    public String offchainLocalStorageGet(StorageKind storageKind, String key) {
        OffchainStore offchainStore = storageByKind(storageKind);
        byte[] bytes = offchainStore.get(new String(StringUtils.hexToBytes(key)));
        if (bytes == null) {
            return null;
        }
        return HEX_PREFIX + HexUtils.toHexString(bytes);
    }

    private OffchainStore storageByKind(StorageKind kind) {
        return switch (kind) {
            case PERSISTENT -> persistentStorage;
            case LOCAL -> localStorage;
        };
    }
}

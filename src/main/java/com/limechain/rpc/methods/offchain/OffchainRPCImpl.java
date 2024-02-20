package com.limechain.rpc.methods.offchain;

import com.limechain.runtime.hostapi.dto.InvalidArgumentException;
import com.limechain.storage.KVRepository;
import com.limechain.storage.offchain.OffchainStore;
import com.limechain.utils.StringUtils;
import org.apache.tomcat.util.buf.HexUtils;
import org.springframework.stereotype.Service;

@Service
public class OffchainRPCImpl {

    private static final String PERSISTENT = "PERSISTENT";
    private static final String LOCAL = "LOCAL";
    private static final String HEX_PREFIX = "0x";
    private final OffchainStore persistentStorage;
    private final OffchainStore localStorage;

    public OffchainRPCImpl(final KVRepository<String, Object> db) {
        persistentStorage = new OffchainStore(db, true);
        localStorage = new OffchainStore(db, false);
    }

    public void offchainLocalStorageSet(String storageKind, String key, String value) {
        OffchainStore offchainStore = storageByKind(storageKind.toUpperCase());
        offchainStore.set(new String(StringUtils.hexToBytes(key)), StringUtils.hexToBytes(value));
    }

    public String offchainLocalStorageGet(String storageKind, String key) {
        OffchainStore offchainStore = storageByKind(storageKind.toUpperCase());
        byte[] bytes = offchainStore.get(new String(StringUtils.hexToBytes(key)));
        if (bytes == null) {
            return null;
        }
        return HEX_PREFIX + HexUtils.toHexString(bytes);
    }

    private OffchainStore storageByKind(String kind) {
        return switch (kind) {
            case PERSISTENT -> persistentStorage;
            case LOCAL -> localStorage;
            default -> throw new InvalidArgumentException("Storage kind", kind);
        };
    }
}

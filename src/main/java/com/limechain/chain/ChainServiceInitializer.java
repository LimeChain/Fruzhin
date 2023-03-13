package com.limechain.chain;

import com.limechain.config.HostConfig;
import com.limechain.storage.KVRepository;

public class ChainServiceInitializer {

    private static ChainService reference;

    // Returns reference to ChainService even if hostConfig and repository is different.
    public static ChainService initialize(HostConfig hostConfig, KVRepository<String, Object> repository) {
        if (reference != null) {
            return reference;
        }
        reference = new ChainService(hostConfig, repository);

        return reference;
    }
}

package org.limechain.chain;

import org.limechain.config.HostConfig;

import java.io.IOException;

public class ChainService {
    public ChainSpec genesis;

    public ChainService (HostConfig hostConfig) {
        try {
            this.genesis = ChainSpec.NewFromJSON(hostConfig.genesisPath);
            System.out.println("✅️Loaded chain spec");
        } catch (IOException e) {
            System.out.println("Failed to load chain spec");
            System.out.println(e.getMessage());
            System.exit(1);
        }
    }
}

package org.limechain.chain;

import org.limechain.config.AppConfig;

import java.io.IOException;

public class ChainService {
    public ChainSpec genesis;

    public ChainService (AppConfig appConfig) {
        try {
            this.genesis = ChainSpec.NewFromJSON(appConfig.genesisPath);
            System.out.println("✅️Loaded chain spec");
        } catch (IOException e) {
            System.out.println("Failed to load chain spec");
            System.exit(1);
        }
    }
}

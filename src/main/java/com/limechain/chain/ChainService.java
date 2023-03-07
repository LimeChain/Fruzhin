package com.limechain.chain;

import com.limechain.config.HostConfig;
import com.limechain.storage.ConfigTable;
import org.rocksdb.RocksDB;

import java.io.IOException;

public class ChainService {
    public ChainSpec genesis;

    public ChainService (HostConfig hostConfig, RocksDB db) {
        try {
            ConfigTable configTable = new ConfigTable(db);
            try {
                System.out.println("Loading chain spec from database");
                this.genesis = configTable.getGenesis();
                System.out.println("✅️Loaded chain spec");
            } catch (ClassNotFoundException | IllegalStateException | IOException e) {
                System.out.println("Error loading chain spec from database. Loading from json...");
                this.genesis = ChainSpec.newFromJSON(hostConfig.genesisPath);
                System.out.println("✅️Loaded chain spec");

                configTable.putGenesis(this.genesis);
                System.out.println("Saved chain spec to database");
            }
        } catch (IOException e) {
            System.out.println("Failed to load chain spec");
            System.out.println(e.getMessage());
            System.exit(1);
        }
    }
}

package com.limechain.chain;

import com.limechain.config.HostConfig;
import com.limechain.storage.ConfigTable;
import org.rocksdb.RocksDB;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ChainService {
    private static final Logger LOGGER = Logger.getLogger(ChainService.class.getName());
    public ChainSpec genesis;

    public ChainService(HostConfig hostConfig, RocksDB db) {
        try {
            ConfigTable configTable = new ConfigTable(db);
            try {
                this.genesis = configTable.getGenesis();
                LOGGER.log(Level.INFO, "✅️Loaded chain spec from DB");
            } catch (IllegalStateException e) {
                this.genesis = ChainSpec.newFromJSON(hostConfig.genesisPath);
                LOGGER.log(Level.INFO, "✅️Loaded chain spec from JSON");

                configTable.putGenesis(this.genesis);
                LOGGER.log(Level.FINE, "Saved chain spec to database");
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load chain spec", e);
            throw new RuntimeException();
        }
    }
}

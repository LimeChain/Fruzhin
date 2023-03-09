package com.limechain.chain;

import com.limechain.config.HostConfig;
import com.limechain.storage.ConfigTable;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
import org.rocksdb.RocksDB;

import java.io.IOException;
import java.util.logging.Level;

@Getter
@Setter
@Log
public class ChainService {
    private ChainSpec genesis;

    public ChainService(HostConfig hostConfig, RocksDB db) {
        try {
            ConfigTable configTable = new ConfigTable(db);
            try {
                this.setGenesis(configTable.getGenesis());
                log.log(Level.INFO, "✅️Loaded chain spec from DB");
            } catch (IllegalStateException e) {
                this.setGenesis(ChainSpec.newFromJSON(hostConfig.getGenesisPath()));
                log.log(Level.INFO, "✅️Loaded chain spec from JSON");

                configTable.putGenesis(this.getGenesis());
                log.log(Level.FINE, "Saved chain spec to database");
            }
        } catch (IOException e) {
            log.log(Level.SEVERE, "Failed to load chain spec", e);
            throw new RuntimeException();
        }
    }
}

package com.limechain.chain;

import com.limechain.config.HostConfig;
import com.limechain.storage.KVRepository;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;

import java.io.IOException;
import java.util.Optional;
import java.util.logging.Level;

/**
 * Service used to read/write chain spec(genesis) info to/from the DB
 */
@Getter
@Setter
@Log
public class ChainService {
    /**
     * Key under which the value is stored
     */
    private final String genesisKey = "genesis";

    /**
     * Key-value repository that stores the chain spec info
     */
    private final KVRepository<String, Object> repository;

    /**
     * Instance that holds the chain spec info
     */
    private ChainSpec genesis;

    public ChainService(HostConfig hostConfig, KVRepository<String, Object> repository) {
        this.repository = repository;
        initialize(hostConfig);
    }

    protected void initialize(HostConfig hostConfig) {
        Optional<Object> genesis = repository.find(this.getGenesisKey());
        if (genesis.isPresent()) {
            this.setGenesis((ChainSpec) genesis.get());
            log.log(Level.INFO, "✅️Loaded chain spec from DB");
            return;
        }

        try {
            this.setGenesis(ChainSpec.newFromJSON(hostConfig.getGenesisPath()));
            log.log(Level.INFO, "✅️Loaded chain spec from JSON");

            repository.save(this.getGenesisKey(), this.getGenesis());
            log.log(Level.FINE, "Saved chain spec to database");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }
}

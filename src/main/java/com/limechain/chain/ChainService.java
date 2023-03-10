package com.limechain.chain;

import com.limechain.config.HostConfig;
import com.limechain.storage.KVRepository;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;

import java.io.IOException;
import java.util.Optional;
import java.util.logging.Level;

@Getter
@Setter
@Log
public class ChainService {
    private final KVRepository<String, Object> repository;
    private ChainSpec genesis;

    public ChainService(HostConfig hostConfig, KVRepository<String, Object> repository) {
        this.repository = repository;

        Optional<Object> genesis = repository.find("genesis");
        if (genesis.isPresent()) {
            this.setGenesis((ChainSpec) genesis.get());
            log.log(Level.INFO, "✅️Loaded chain spec from DB");
            return;
        }

        try {
            this.setGenesis(ChainSpec.newFromJSON(hostConfig.getGenesisPath()));
            log.log(Level.INFO, "✅️Loaded chain spec from JSON");

            repository.save("genesis", this.getGenesis());
            log.log(Level.FINE, "Saved chain spec to database");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

package com.limechain.chain;

import com.limechain.config.HostConfig;
import com.limechain.storage.DBConstants;
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
     * Key-value repository that stores the chain spec info
     */
    private final KVRepository<String, Object> repository;

    /**
     * Instance that holds the chain spec info
     */
    private ChainSpec genesis;

    /**
     * Whether the chain setup is in local development mode
     */
    private boolean isLocalChain;

    public ChainService(HostConfig hostConfig, KVRepository<String, Object> repository) {
        this.repository = repository;
        initialize(hostConfig);
    }

    protected void initialize(HostConfig hostConfig) {
        Optional<Object> genesis = repository.find(DBConstants.GENESIS_KEY);
        /*
            WORKAROUND
            The inLocalDevelopment variable and its usage below are only to aid in development.
            It is expected that the local genesis file will change rather frequently while the all official
                chain specifications will never change. To improve performance we are loading the chain
                specifications only once, and then they are saved to the database.
            Saving the local genesis file is not suitable in this early phase of development.
            This might be removed in the future.
         */
        isLocalChain = hostConfig.getChain() == Chain.LOCAL;
        if (genesis.isPresent() && !isLocalChain) {
            this.setGenesis((ChainSpec) genesis.get());
            log.log(Level.INFO, "✅️Loaded chain spec from DB");
            return;
        }

        try {
            this.setGenesis(ChainSpec.newFromJSON(hostConfig.getGenesisPath()));
            log.log(Level.INFO, "✅️Loaded chain spec from JSON");

            repository.save(DBConstants.GENESIS_KEY, this.getGenesis());
            log.log(Level.FINE, "Saved chain spec to database");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public boolean isChainLive() {
        String chainType = this.getGenesis().getChainType();
        if (chainType != null) {
            return chainType.equals("Live");
        }
        return !isLocalChain;
    }
}

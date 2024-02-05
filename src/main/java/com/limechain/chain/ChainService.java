package com.limechain.chain;

import com.limechain.chain.spec.ChainSpec;
import com.limechain.chain.spec.ChainType;
import com.limechain.config.HostConfig;
import com.limechain.exception.ChainServiceInitializationException;
import com.limechain.storage.DBConstants;
import com.limechain.storage.KVRepository;
import lombok.Getter;
import lombok.extern.java.Log;

import java.io.IOException;
import java.util.Optional;
import java.util.logging.Level;

/**
 * Service used to read/write chain spec(genesis) info to/from the DB
 */
@Getter
@Log
public class ChainService {
    /**
     * Key-value repository that stores the chain spec info
     */
    private final KVRepository<String, Object> repository;

    /**
     * Instance that holds the chain spec info
     */
    private final ChainSpec chainSpec;

    /**
     * Whether the chain setup is in local development mode
     */
    private final boolean isLocalChain;

    public ChainService(HostConfig hostConfig, KVRepository<String, Object> repository) {
        this.repository = repository;
        this.isLocalChain = hostConfig.getChain() == Chain.LOCAL;

        /*
            WORKAROUND
            The inLocalDevelopment variable and its usage below are only to aid in development.
            It is expected that the local genesis file will change rather frequently while the all official
                chain specifications will never change. To improve performance we are loading the chain
                specifications only once, and then they are saved to the database.
            Saving the local genesis file is not suitable in this early phase of development.
            This might be removed in the future.
         */
        // TODO: Think about extracting this initialization logic outside this constructor
        Optional<Object> cachedChainSpec = repository.find(DBConstants.GENESIS_KEY);

        if (cachedChainSpec.isPresent() && !isLocalChain) {
            this.chainSpec = (ChainSpec) cachedChainSpec.get();
            log.log(Level.INFO, "✅️Loaded chain spec from DB");
        } else {
            try {
                this.chainSpec = ChainSpec.newFromJSON(hostConfig.getGenesisPath());
                log.log(Level.INFO, "✅️Loaded chain spec from JSON");

                repository.save(DBConstants.GENESIS_KEY, this.chainSpec);
                log.log(Level.FINE, "Saved chain spec to database");
            } catch (IOException e) {
                throw new ChainServiceInitializationException(e);
            }
        }
    }

    public boolean isChainLive() {
        ChainType chainType = this.chainSpec.getChainType();
        if (chainType != null) {
            return chainType == ChainType.LIVE;
        }
        return !isLocalChain;
    }
}

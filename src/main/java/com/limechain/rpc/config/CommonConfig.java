package com.limechain.rpc.config;

import com.limechain.chain.ChainService;
import com.limechain.config.HostConfig;
import com.limechain.config.SystemInfo;
import com.limechain.network.Network;
import com.limechain.storage.DBInitializer;
import com.limechain.storage.KVRepository;
import com.limechain.storage.block.SyncState;
import com.limechain.sync.warpsync.WarpSyncMachine;
import com.limechain.sync.warpsync.WarpSyncState;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Spring configuration class used to instantiate beans.
 */
@Configuration
@EnableScheduling
public class CommonConfig {

    @Bean
    public HostConfig hostConfig() {
        return new HostConfig();
    }

    @Bean
    public KVRepository<String, Object> repository(HostConfig hostConfig) {
        return DBInitializer.initialize(hostConfig.getChain());
    }

    @Bean
    public ChainService chainService(HostConfig hostConfig, KVRepository<String, Object> repository) {
        return new ChainService(hostConfig, repository);
    }

    @Bean
    public SyncState syncState(KVRepository<String, Object> repository) {
        return new SyncState(repository);
    }

    @Bean
    public SystemInfo systemInfo(HostConfig hostConfig, Network network, SyncState syncState) {
        return new SystemInfo(hostConfig, network, syncState);
    }

    @Bean
    public Network network(ChainService chainService, HostConfig hostConfig, KVRepository<String, Object> repository) {
        return new Network(chainService, hostConfig, repository);
    }

    @Bean
    public WarpSyncState warpSyncState(Network network, SyncState syncState,
                                       KVRepository<String, Object> repository) {
        return new WarpSyncState(syncState, network, repository);
    }

    @Bean
    public WarpSyncMachine warpSyncMachine(Network network, ChainService chainService, SyncState syncState,
                                           WarpSyncState warpSyncState) {
        return new WarpSyncMachine(network, chainService, syncState, warpSyncState);
    }

}

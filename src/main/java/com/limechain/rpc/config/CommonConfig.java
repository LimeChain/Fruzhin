package com.limechain.rpc.config;

import com.googlecode.jsonrpc4j.spring.AutoJsonRpcServiceImplExporter;
import com.limechain.chain.ChainService;
import com.limechain.cli.Cli;
import com.limechain.cli.CliArguments;
import com.limechain.config.HostConfig;
import com.limechain.config.SystemInfo;
import com.limechain.network.Network;
import com.limechain.storage.DBInitializer;
import com.limechain.storage.KVRepository;
import com.limechain.sync.warpsync.WarpSyncMachine;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration class used to instantiate beans.
 */
@Configuration
public class CommonConfig {
    @Bean
    public static AutoJsonRpcServiceImplExporter autoJsonRpcServiceImplExporter() {
        AutoJsonRpcServiceImplExporter exp = new AutoJsonRpcServiceImplExporter();
        // in here you can provide custom HTTP status code providers etc. eg:
        // exp.setHttpStatusCodeProvider();
        // exp.setErrorResolver();
        return exp;
    }

    @Bean
    public HostConfig hostConfig(ApplicationArguments arguments) {
        CliArguments cliArgs = new Cli().parseArgs(arguments.getSourceArgs());
        return new HostConfig(cliArgs);
    }

    @Bean
    public KVRepository<String, Object> repository(HostConfig hostConfig) {
        return DBInitializer.initialize(hostConfig.getRocksDbPath(), hostConfig.getChain(), hostConfig.isDbRecreate());
    }

    @Bean
    public ChainService chainService(HostConfig hostConfig, KVRepository<String, Object> repository) {
        return new ChainService(hostConfig, repository);
    }

    @Bean
    public SystemInfo systemInfo(HostConfig hostConfig, Network network) {
        return new SystemInfo(hostConfig, network);
    }

    @Bean
    public Network network(ChainService chainService, HostConfig hostConfig) {
        return Network.initialize(chainService, hostConfig);
    }

    @Bean
    public WarpSyncMachine sync(Network network, ChainService chainService) {
        return WarpSyncMachine.initialize(network, chainService);
    }
}

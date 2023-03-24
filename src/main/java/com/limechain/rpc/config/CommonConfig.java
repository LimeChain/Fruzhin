package com.limechain.rpc.config;

import com.googlecode.jsonrpc4j.spring.AutoJsonRpcServiceImplExporter;
import com.limechain.chain.ChainService;
import com.limechain.cli.Cli;
import com.limechain.cli.CliArguments;
import com.limechain.config.HostConfig;
import com.limechain.config.SystemInfo;
import com.limechain.network.Network;
import com.limechain.rpc.ws.client.WebSocketClient;
import com.limechain.storage.DBInitializer;
import com.limechain.storage.KVRepository;
import io.libp2p.core.Host;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
        return DBInitializer.initialize(hostConfig.getRocksDbPath());
    }

    @Bean
    public ChainService chainService(HostConfig hostConfig, KVRepository<String, Object> repository) {
        return new ChainService(hostConfig, repository);
    }

    @Bean
    public SystemInfo systemInfo(HostConfig hostConfig) {
        return new SystemInfo();
    }

    @Bean
    public Network network(ChainService chainService, HostConfig hostConfig) {
        return Network.initialize(chainService, hostConfig);
    }

    @Bean
    public WebSocketClient wsClient(HostConfig hostConfig) {
        return new WebSocketClient(hostConfig.getHelperNodeAddress());
    }

}

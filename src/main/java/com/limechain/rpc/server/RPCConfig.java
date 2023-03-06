package com.limechain.rpc.server;

import com.googlecode.jsonrpc4j.spring.AutoJsonRpcServiceImplExporter;
import com.limechain.chain.ChainService;
import com.limechain.config.HostConfig;
import com.limechain.config.SystemInfo;
import com.limechain.storage.RocksDBInitializer;
import com.limechain.ws.client.WebSocketClient;
import org.rocksdb.RocksDB;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RPCConfig {
    @Bean
    public static AutoJsonRpcServiceImplExporter autoJsonRpcServiceImplExporter () {
        AutoJsonRpcServiceImplExporter exp = new AutoJsonRpcServiceImplExporter();
        // in here you can provide custom HTTP status code providers etc. eg:
        // exp.setHttpStatusCodeProvider();
        // exp.setErrorResolver();
        return exp;
    }

    @Bean
    public HostConfig hostConfig (ApplicationArguments arguments) {
        String[] commandLineArguments = arguments.getSourceArgs();
        System.out.println(commandLineArguments);
        return new HostConfig(commandLineArguments);
    }

    @Bean
    public ChainService chainService (HostConfig hostConfig, RocksDB db) {
        return new ChainService(hostConfig, db);
    }

    @Bean
    public RocksDB rocksDb (HostConfig hostConfig) {
        return RocksDBInitializer.initialize(hostConfig);
    }

    @Bean
    public SystemInfo systemInfo (HostConfig hostConfig) {
        return new SystemInfo();
    }

    @Bean
    public WebSocketClient wsClient (HostConfig hostConfig) {
        return new WebSocketClient(hostConfig.helperNodeAddress);
    }

}

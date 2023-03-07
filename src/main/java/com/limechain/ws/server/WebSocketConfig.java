package com.limechain.ws.server;

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
public class WebSocketConfig {
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
        return new HostConfig(arguments.getSourceArgs());
    }

    @Bean
    public ChainService chainService (HostConfig hostConfig, RocksDB rocksDb) {
        return new ChainService(hostConfig, rocksDb);
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

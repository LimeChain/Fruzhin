package com.limechain.rpc.ws.server;

import com.limechain.config.HostConfig;
import com.limechain.rpc.methods.RPCMethods;
import com.limechain.rpc.subscriptions.chainhead.ChainHeadRpc;
import com.limechain.rpc.subscriptions.chainhead.ChainHeadRpcImpl;
import com.limechain.rpc.subscriptions.transaction.TransactionRpc;
import com.limechain.rpc.subscriptions.transaction.TransactionRpcImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketRoutingConfig implements WebSocketConfigurer {
    // These dependencies will be injected from the common configuration
    private final RPCMethods rpcMethods;
    private final HostConfig hostConfig;

    public WebSocketRoutingConfig(RPCMethods rpcMethods, HostConfig hostConfig) {
        this.rpcMethods = rpcMethods;
        this.hostConfig = hostConfig;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketHandler(), "/");
    }

    public WebSocketHandler webSocketHandler() {
        return new WebSocketHandler(rpcMethods, chainHeadRpc(), transactionRpc());
    }

    @Bean
    public ChainHeadRpc chainHeadRpc() {
        return new ChainHeadRpcImpl(hostConfig.getHelperNodeAddress());
    }

    @Bean
    public TransactionRpc transactionRpc() {
        return new TransactionRpcImpl(hostConfig.getHelperNodeAddress());
    }

}

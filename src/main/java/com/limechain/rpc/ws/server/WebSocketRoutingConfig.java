package com.limechain.rpc.ws.server;

import com.limechain.rpc.methods.RPCMethods;
import com.limechain.rpc.ws.pubsub.PubSubService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketRoutingConfig implements WebSocketConfigurer {
    private final RPCMethods rpcMethods;
    private final PubSubService pubSubService;

    public WebSocketRoutingConfig(RPCMethods rpcMethods, PubSubService pubSubService) {
        this.rpcMethods = rpcMethods;
        this.pubSubService = pubSubService;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketHandler(), "/");
    }

    public WebSocketHandler webSocketHandler() {
        return new WebSocketHandler(rpcMethods, pubSubService);
    }
}

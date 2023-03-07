package com.limechain.rpc.ws.server;

import com.limechain.rpc.methods.RPCMethods;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketRoutingConfig implements WebSocketConfigurer {
    private final RPCMethods rpcMethods;

    public WebSocketRoutingConfig (RPCMethods rpcMethods) {
        this.rpcMethods = rpcMethods;
    }

    @Override
    public void registerWebSocketHandlers (WebSocketHandlerRegistry registry) {
        registry.addHandler(webSocketHandler(), "/");
    }

    public WebSocketHandler webSocketHandler () {
        return new WebSocketHandler(rpcMethods);
    }
}

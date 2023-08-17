package com.limechain.rpc.config;

import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.socket.server.support.WebSocketHandlerMapping;

/**
 * Spring configuration class used to configure the websocket handler mapping to process only websocket requests or http requests with upgrade headers only
 *
 * <p>
 *     The configuration is in separate file from {@link CommonConfig} because of the circular dependency loop
 */
@Configuration
@Log
public class RpcConfig {

    public RpcConfig(@Qualifier("webSocketHandlerMapping") HandlerMapping webSocketHandlerMapping) {
        WebSocketHandlerMapping webSocketHandlerMapping1 = (WebSocketHandlerMapping) webSocketHandlerMapping;
        webSocketHandlerMapping1.setWebSocketUpgradeMatch(true);
    }

}

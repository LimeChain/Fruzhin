package com.limechain.rpc.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.socket.server.support.WebSocketHandlerMapping;

@Configuration
public class WebsocketConfig {

    public WebsocketConfig(@Qualifier("webSocketHandlerMapping") HandlerMapping webSocketHandlerMapping) {
        WebSocketHandlerMapping webSocketHandlerMapping1 = (WebSocketHandlerMapping) webSocketHandlerMapping;
        webSocketHandlerMapping1.setWebSocketUpgradeMatch(true);
    }

}

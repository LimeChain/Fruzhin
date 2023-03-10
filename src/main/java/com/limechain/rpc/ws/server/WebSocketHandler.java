package com.limechain.rpc.ws.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.jsonrpc4j.JsonRpcBasicServer;
import com.limechain.rpc.methods.RPCMethods;
import com.limechain.rpc.ws.pubsub.PubSubService;
import com.limechain.rpc.ws.pubsub.Topic;
import lombok.extern.java.Log;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Level;

@Component
@Log
public class WebSocketHandler extends TextWebSocketHandler {

    private final JsonRpcBasicServer server;
    private final PubSubService pubSubService;

    public WebSocketHandler(RPCMethods rpcMethods, PubSubService pubSubService) {
        ObjectMapper mapper = new ObjectMapper();
        this.server = new JsonRpcBasicServer(mapper, rpcMethods);
        this.pubSubService = pubSubService;
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        // Known issue: WS handler doesn't use interface
        // method names (system_name) but uses implementation ones (systemName)
        System.out.println("SESSION ID: " + session.getId());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream in = new ByteArrayInputStream(message.asBytes());
        log.log(Level.INFO, "Handling ws request");

        JsonNode result = new ObjectMapper().readValue(in, JsonNode.class);
        String method = result.get("method").toString();
        switch (method) {
            case "\"chainHeadUnstableFollow\"" -> {
                log.log(Level.INFO, "Subscribing for follow event");
                pubSubService.addSubscriber(Topic.UNSTABLE_FOLLOW, session);
            }
            case "\"chainUnstableUnfollow\"" -> {
                log.log(Level.INFO, "Unsubscribing from follow event");
                pubSubService.removeSubscriber(Topic.UNSTABLE_FOLLOW, session.getId());
            }
            default -> {
                // Server should only handle requests if it's not a sub/unsub request
                log.log(Level.INFO, "Handling the WS request using the normal RPC routes");
                server.handleRequest(in, out);
                session.sendMessage(new TextMessage(out.toByteArray()));
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        //TODO: Search PubSubService for subscribers that have the closed session id
        // as a subscriber and remove it from the their subscription list
    }
}

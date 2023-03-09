package com.limechain.rpc.ws.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.jsonrpc4j.JsonRpcBasicServer;
import com.limechain.rpc.methods.RPCMethods;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class WebSocketHandler extends TextWebSocketHandler {

    private final JsonRpcBasicServer server;
    private final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();

    public WebSocketHandler(RPCMethods rpcMethods) {
        ObjectMapper mapper = new ObjectMapper();
        this.server = new JsonRpcBasicServer(mapper, rpcMethods);
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream in = new ByteArrayInputStream(message.asBytes());
        // Known issue: WS handler doesn't use interface
        // method names (system_name) but uses implementation ones (systemName)
        server.handleRequest(in, out);
        session.sendMessage(new TextMessage(out.toByteArray()));

    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // The messages will be broadcast to all users.
        sessions.add(session);
    }
}

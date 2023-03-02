package org.limechain.rpc.ws.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.jsonrpc4j.JsonRpcBasicServer;
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
public class SocketHandler extends TextWebSocketHandler {

    private final JsonRpcBasicServer server;
    List sessions = new CopyOnWriteArrayList<>();

    public SocketHandler () {
        ObjectMapper mapper = new ObjectMapper();
        Object handler = new WebSocketRPCImpl();
        this.server = new JsonRpcBasicServer(mapper, handler);
    }


    @Override
    public void handleTextMessage (WebSocketSession session, TextMessage message) throws InterruptedException, IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        InputStream in = new ByteArrayInputStream(message.asBytes());
        server.handleRequest(in, out);
        session.sendMessage(new TextMessage(out.toByteArray()));

    }

    @Override
    public void afterConnectionEstablished (WebSocketSession session) throws Exception {
        //the messages will be broadcasted to all users.
        sessions.add(session);
    }
}

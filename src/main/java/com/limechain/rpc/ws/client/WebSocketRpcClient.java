package com.limechain.rpc.ws.client;

import lombok.extern.java.Log;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.logging.Level;

@Log
public class WebSocketRpcClient extends WebSocketClient {

    public WebSocketRpcClient(URI serverURI) {
        super(serverURI);
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        log.log(Level.INFO, "new WS connection opened");
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        log.log(Level.INFO, "closed with exit code " + code + " additional info: " + reason);
    }

    @Override
    public void onMessage(String message) {
        log.log(Level.FINE, "received message: " + message);
    }

    @Override
    public void onMessage(ByteBuffer message) {
        log.log(Level.FINE, "received ByteBuffer");
    }

    @Override
    public void onError(Exception ex) {
        log.log(Level.SEVERE, "an error occurred:", ex);
    }

    public void send(String method, String[] params) {
        String message =
                "{\"id\":1,\"jsonrpc\":\"2.0\",\"method\":\"" + method + "\",\"params\":[" + String.join(",", params) +
                        "]}";
        super.send(message);
    }
}

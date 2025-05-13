package com.limechain.rpc.client;

import lombok.extern.java.Log;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.logging.Level;

/**
 * Extension class for {@link WebSocketClient}. Able to send RPC messages and log incoming data
 */
@Log
public abstract class AbstractRpcClient extends WebSocketClient {
    public static final String REG_EXPRESSION_NUMBER = "-?\\d+(\\.\\d+)?";

    protected AbstractRpcClient(URI serverURI) {
        super(serverURI);
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        log.log(Level.FINE, "new WS connection opened");
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

    /**
     * Creates a json rpc request and sends it to the server
     *
     * @param method method to be invoked
     * @param params method parameters
     */
    public void send(String method, String[] params) {
        String payload = buildRpcRequest(method, params);
        super.send(payload);
    }

    private String buildRpcRequest(String method, String[] params) {
        StringBuilder builder = new StringBuilder();
        builder.append("{")
                .append("\"id\":1, ")
                .append("\"jsonrpc\":\"2.0\", ")
                .append("\"method\":\"").append(method).append("\", ")
                .append("\"params\":[");

        for (int i = 0; i < params.length; i++) {
            builder.append(serializeParam(params[i]));
            if (i < params.length - 1) {
                builder.append(", ");
            }
        }

        builder.append("]}");
        return builder.toString();
    }

    private String serializeParam(String param) {
        if (param == null) {
            return "null";
        }
        if (isNumeric(param)) {
            return param;
        }
        return "\"" + param + "\"";
    }


    private boolean isNumeric(String str) {
        return str.matches(REG_EXPRESSION_NUMBER);
    }
}

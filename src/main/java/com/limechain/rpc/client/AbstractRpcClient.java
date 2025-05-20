package com.limechain.rpc.client;

import lombok.extern.java.Log;
import org.apache.commons.lang3.StringUtils;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.nio.ByteBuffer;

/**
 * Extension class for {@link WebSocketClient}. Able to send RPC messages and log incoming data
 */
@Log
public abstract class AbstractRpcClient extends WebSocketClient {

    protected AbstractRpcClient(URI serverURI) {
        super(serverURI);
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        log.fine("new WS connection opened");
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        log.info(String.format("closed with exit code %d additional info: %s", code, reason));
    }

    @Override
    public void onMessage(String message) {
        log.fine(String.format("received message: %s", message));
    }

    @Override
    public void onMessage(ByteBuffer message) {
        log.fine("received ByteBuffer");
    }

    @Override
    public void onError(Exception ex) {
        log.severe(String.format("an error occurred: %s", ex.getMessage()));
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
        if (StringUtils.isNumeric(param)) {
            return param;
        }
        return "\"" + param + "\"";
    }
}

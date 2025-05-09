package com.limechain.rpc.client;

import lombok.extern.java.Log;
import org.java_websocket.client.WebSocketClient;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Log
public class StateSyncRpcClient extends AbstractRpcClient {
    private final Map<WebSocketClient, CompletableFuture<String>> responseMap;

    public StateSyncRpcClient(URI serverUri,
                              Map<WebSocketClient, CompletableFuture<String>> responseMap) {
        super(serverUri);
        this.responseMap = responseMap;
    }

    @Override
    public void onMessage(String message) {
        CompletableFuture<String> future = responseMap.remove(this);
        if (future != null && !future.isDone()) future.complete(message);
    }

    @Override
    public void onError(Exception ex) {
        CompletableFuture<String> future = responseMap.remove(this);
        if (future != null && !future.isDone()) future.completeExceptionally(ex);
    }


    @Override
    public void send(String method, String[] params) {
        StringBuilder builder = new StringBuilder();
        builder.append("{\"id\":1, \"jsonrpc\":\"2.0\", \"method\": \"")
                .append(method)
                .append("\", \"params\":[");

        for (int i = 0; i < params.length; i++) {
            String param = params[i];
            if (param == null) {
                builder.append("null");
            } else if (param.matches("-?\\d+(\\.\\d+)?")) { // check if it's a number
                builder.append(param);
            } else {
                builder.append("\"").append(param).append("\"");
            }

            if (i < params.length - 1) {
                builder.append(", ");
            }
        }

        builder.append("]}");
        super.send(builder.toString());
    }
}

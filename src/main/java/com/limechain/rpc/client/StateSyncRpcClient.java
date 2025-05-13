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
}

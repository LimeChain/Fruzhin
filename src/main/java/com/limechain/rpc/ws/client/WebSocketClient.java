package com.limechain.rpc.ws.client;

import io.emeraldpay.polkaj.api.PolkadotApi;
import io.emeraldpay.polkaj.api.SubscribeCall;
import io.emeraldpay.polkaj.api.Subscription;
import io.emeraldpay.polkaj.apiws.JavaHttpSubscriptionAdapter;

import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class WebSocketClient {

    private final String helperNodeAddress;
    private PolkadotApi api;

    public WebSocketClient(String helperNodeAddress) {
        this.helperNodeAddress = helperNodeAddress;
    }

    public WebSocketClient connect() {
        try {
            JavaHttpSubscriptionAdapter wsAdapter = JavaHttpSubscriptionAdapter
                    .newBuilder()
                    // Currently requires smoldot node to be running
                    .connectTo(helperNodeAddress)
                    .build();

            PolkadotApi api = PolkadotApi
                    .newBuilder()
                    .subscriptionAdapter(wsAdapter)
                    .build();

            // IMPORTANT! connect to the node as the first step before making calls or subscriptions.

            wsAdapter.connect().get(5, TimeUnit.SECONDS);
            this.api = api;
            return this;
        } catch (URISyntaxException | InterruptedException | ExecutionException | RuntimeException |
                 TimeoutException e) {
            throw new RuntimeException(e);
        }

    }

    public <T> Subscription<T> subscribeToEvent(SubscribeCall<T> subscriptionCall) {
        try {
            Future<Subscription<T>> hashFuture = api.subscribe(subscriptionCall);
            Subscription<T> subscription = null;
            subscription = hashFuture.get(30, TimeUnit.SECONDS);
            return subscription;
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }
}

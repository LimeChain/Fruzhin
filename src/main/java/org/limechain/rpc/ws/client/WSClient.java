package org.limechain.rpc.ws.client;

import io.emeraldpay.polkaj.api.PolkadotApi;
import io.emeraldpay.polkaj.api.SubscribeCall;
import io.emeraldpay.polkaj.api.Subscription;
import io.emeraldpay.polkaj.apiws.JavaHttpSubscriptionAdapter;

import java.net.URISyntaxException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class WSClient {

    private final PolkadotApi api;

    public WSClient () {
        try {
            JavaHttpSubscriptionAdapter wsAdapter = JavaHttpSubscriptionAdapter
                    .newBuilder()
                    // Currently requires smoldot node to be running
                    // Should be passed from app.config
                    .connectTo("ws://127.0.0.1:9944/westend2")
                    .build();
            PolkadotApi api = PolkadotApi
                    .newBuilder()
                    .subscriptionAdapter(wsAdapter)
                    .build();

            // IMPORTANT! connect to the node as the first step before making calls or subscriptions.

            wsAdapter.connect().get(5, TimeUnit.SECONDS);
            this.api = api;
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    public <T> Subscription<T> subscribeToEvent (SubscribeCall<T> subscriptionCall) {
        try {
            Future<Subscription<T>> hashFuture = api.subscribe(subscriptionCall);
            Subscription<T> subscription = null;
            subscription = hashFuture.get(30, TimeUnit.SECONDS);
            return subscription;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        }
    }
}

package com.limechain.rpc.subscriptions.transaction;

import com.limechain.rpc.config.SubscriptionName;
import com.limechain.rpc.pubsub.Topic;
import com.limechain.rpc.pubsub.publisher.PublisherImpl;
import com.limechain.rpc.subscriptions.utils.Utils;
import com.limechain.rpc.client.SubscriptionRpcClient;

import java.net.URI;
import java.net.URISyntaxException;

public class TransactionRpcImpl implements TransactionRpc {

    /**
     * WebSocket client which forwards the requests to smoldot
     */
    private final SubscriptionRpcClient wsClient;

    public TransactionRpcImpl(String forwardNodeAddress) {
        try {
            this.wsClient = new SubscriptionRpcClient(new URI(forwardNodeAddress), new PublisherImpl(),
                    Topic.UNSTABLE_TRANSACTION_WATCH);
            // TODO: Move connect outside constructor
            wsClient.connectBlocking();
        } catch (URISyntaxException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void transactionUnstableSubmitAndWatch(String transaction) {
        wsClient.send(SubscriptionName.TRANSACTION_UNSTABLE_SUBMIT_AND_WATCH.getValue(),
                new String[]{Utils.wrapWithDoubleQuotes(transaction)});

    }

    @Override
    public void transactionUnstableUnwatch(String subscriptionId) {
        wsClient.send(SubscriptionName.TRANSACTION_UNSTABLE_UNWATCH.getValue(),
                new String[]{Utils.wrapWithDoubleQuotes(subscriptionId)});
    }

}

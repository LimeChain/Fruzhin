package com.limechain.rpc.subscriptions.chainhead;

import com.limechain.rpc.config.SubscriptionName;
import com.limechain.rpc.pubsub.Topic;
import com.limechain.rpc.pubsub.publisher.PublisherImpl;
import com.limechain.rpc.ws.client.SubscriptionRpcClient;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;

@Service
public class ChainHeadRpcImpl implements ChainHeadRpc {
    private final SubscriptionRpcClient wsClient;

    public ChainHeadRpcImpl(String forwardNodeAddress) {
        try {
            this.wsClient = new SubscriptionRpcClient(new URI(forwardNodeAddress), new PublisherImpl(),
                    Topic.UNSTABLE_FOLLOW);
            //TODO: Move connect outside constructor
            wsClient.connectBlocking();
        } catch (URISyntaxException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void chainUnstableFollow(boolean runtimeUpdates) {
        wsClient.send(SubscriptionName.CHAIN_HEAD_UNSTABLE_FOLLOW.getValue(),
                new String[]{String.valueOf(runtimeUpdates)});
    }

    public void chainUnstableUnfollow(String subscriptionId) {
        // Weird workaround because "0" is passed as 0 in the params which breaks request
        wsClient.send(SubscriptionName.CHAIN_HEAD_UNSTABLE_UNFOLLOW.getValue(),
                new String[]{'"' + subscriptionId + '"'});
    }

    public void chainUnstableUnpin() {
    }

    public void chainUnstableStorage() {
    }

    public void chainUnstableCall() {
    }

    public void chainUnstableStopCall() {
    }
}

package com.limechain.rpc.subscriptions.chainhead;

import com.limechain.rpc.pubsub.Topic;
import com.limechain.rpc.pubsub.publisher.PublisherImpl;
import com.limechain.rpc.ws.client.SubscriptionClient;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;

@Service
public class ChainHeadRpcImpl {
    private final SubscriptionClient wsClient;

    public ChainHeadRpcImpl(String forwardNodeAddress) {
        try {
            this.wsClient = new SubscriptionClient(new URI(forwardNodeAddress), new PublisherImpl(),
                    Topic.UNSTABLE_FOLLOW);
            // Move connect outside constructor
            wsClient.connectBlocking();
        } catch (URISyntaxException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void chainUnstableFollow(boolean runtimeUpdates) {
        wsClient.send("chainHead_unstable_follow", new String[]{String.valueOf(runtimeUpdates)});
    }


    public void chainUnstableUnfollow(String subscriptionId) {
        // Weird workaround because "0" is passed as 0 in the params which breaks request
        wsClient.send("chainHead_unstable_unfollow", new String[]{'"' + subscriptionId + '"'});
    }

    public String chainUnstableUnpin() {
        return null;
    }

    public String chainUnstableStorage() {
        return null;
    }

    public String chainUnstableCall() {
        return null;
    }

    public String chainUnstableStopCall() {
        return null;
    }
}

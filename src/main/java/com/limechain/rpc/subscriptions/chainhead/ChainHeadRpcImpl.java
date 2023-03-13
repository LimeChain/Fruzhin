package com.limechain.rpc.subscriptions.chainhead;

import com.limechain.rpc.pubsub.PubSubService;
import com.limechain.rpc.pubsub.Topic;
import com.limechain.rpc.pubsub.publisher.Publisher;
import com.limechain.rpc.pubsub.publisher.PublisherImpl;
import com.limechain.rpc.ws.client.ChainHeadFollowClient;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;

@Service
public class ChainHeadRpcImpl {
    private final ChainHeadFollowClient wsClient;
    private final PubSubService pubSubService = PubSubService.getInstance();
    private final Publisher chainPublisher = new PublisherImpl();

    public ChainHeadRpcImpl(String forwardNodeAddress) {
        try {
            this.wsClient = new ChainHeadFollowClient(new URI(forwardNodeAddress), chainPublisher,
                    Topic.UNSTABLE_FOLLOW);
            wsClient.connectBlocking();
        } catch (URISyntaxException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void chainUnstableFollow(boolean runtimeUpdates) {
        wsClient.send("chainHead_unstable_follow", new String[]{String.valueOf(runtimeUpdates)});
    }


    public void chainUnstableUnfollow(String subscriptionId) {
        wsClient.send("chainHead_unstable_unfollow", new String[]{subscriptionId});
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

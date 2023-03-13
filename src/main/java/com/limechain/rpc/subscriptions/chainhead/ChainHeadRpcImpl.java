package com.limechain.rpc.subscriptions.chainhead;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.limechain.rpc.pubsub.Message;
import com.limechain.rpc.pubsub.PubSubService;
import com.limechain.rpc.pubsub.Topic;
import com.limechain.rpc.pubsub.publisher.Publisher;
import com.limechain.rpc.pubsub.publisher.PublisherImpl;
import com.limechain.rpc.subscriptions.SubscriptionCalls;
import com.limechain.rpc.subscriptions.chainhead.events.FollowEvent;
import com.limechain.rpc.utils.RpcResponse;
import com.limechain.rpc.ws.client.WebSocketClient;
import io.emeraldpay.polkaj.api.Subscription;
import org.springframework.stereotype.Service;

@Service
public class ChainHeadRpcImpl {
    private final WebSocketClient wsClient;
    private final PubSubService pubSubService = PubSubService.getInstance();
    private final Publisher chainPublisher = new PublisherImpl();
    private final ObjectMapper mapper = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    public ChainHeadRpcImpl(WebSocketClient wsClient) {
        this.wsClient = wsClient;
    }

    public void chainUnstableFollow(boolean runtimeUpdates) {
        // TODO: Research how to retrieve subscription id from response
        Subscription<FollowEvent> subscription =
                this.wsClient.connect().subscribeToEvent(SubscriptionCalls.unstableFollow(runtimeUpdates));

        subscription.handler((Subscription.Event<FollowEvent> event) -> {
            FollowEvent header = event.getResult();
            String response = RpcResponse.create("2.0", "1", mapper.valueToTree(header));
            System.out.println(response);
            chainPublisher.publish(new Message(Topic.UNSTABLE_FOLLOW.getValue(), response), pubSubService);

            // This will send messages to all subscriber channels
            // illustrating that publishing and broadcasting messages are 2 independent processes
            // broadcast() can be called from a "manager" class if fine-grained control
            // is needed (probably will be in the future)
            pubSubService.broadcast();

            // This will send all accumulated messages in each subscriber channel
            // to all the subscribers(sessions)
            // This is an example that notifications are decoupled from
            // broadcasting messages to subscriber channels
            pubSubService.notifySubscribers();
        });
    }


    public String chainUnstableUnfollow(String sessionId) {
        pubSubService.removeSubscriber(Topic.UNSTABLE_FOLLOW, sessionId);
        return sessionId;
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

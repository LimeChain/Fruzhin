package com.limechain.rpc.ws.client;

import com.limechain.rpc.pubsub.Message;
import com.limechain.rpc.pubsub.PubSubService;
import com.limechain.rpc.pubsub.Topic;
import com.limechain.rpc.pubsub.publisher.Publisher;

import java.net.URI;

public class SubscriptionClient extends BaseClient {
    private final Publisher chainPublisher;
    private final PubSubService pubSubService = PubSubService.getInstance();
    private final Topic topic;

    public SubscriptionClient(URI serverURI, Publisher chainPublisher, Topic topic) {
        super(serverURI);
        this.chainPublisher = chainPublisher;
        this.topic = topic;
    }

    @Override
    public void onMessage(String message) {
        // For now, we'll be forwarding the message we've received
        // In the future, after sync module is completed
        // this functionality will be replaced with self-emitting events
        chainPublisher.publish(new Message(topic.getValue(), message), pubSubService);

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
    }

}
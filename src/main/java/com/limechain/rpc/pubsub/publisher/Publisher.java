package com.limechain.rpc.pubsub.publisher;

import com.limechain.rpc.pubsub.Message;
import com.limechain.rpc.pubsub.PubSubService;

// Publishers are the entities who create/publish a message on a topic.
public interface Publisher {
    // Publishes new message to PubSubService
    void publish(Message message, PubSubService pubSubService);
}

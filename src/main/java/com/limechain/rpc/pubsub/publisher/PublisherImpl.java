package com.limechain.rpc.pubsub.publisher;

import com.limechain.rpc.pubsub.Message;
import com.limechain.rpc.pubsub.PubSubService;

public class PublisherImpl implements Publisher {
    //Publishes new message to PubSubService
    public void publish(Message message, PubSubService pubSubService) {
        pubSubService.addMessageToQueue(message);
    }
}


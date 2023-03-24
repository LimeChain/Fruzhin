package com.limechain.rpc.pubsub.publisher;

import com.limechain.rpc.pubsub.Message;
import com.limechain.rpc.pubsub.PubSubService;

/**
 * Publishers are the entities who create/publish a message on a topic.
 */
public class PublisherImpl implements Publisher {

    /**
     * Publishes new message to PubSubService
     *
     * @param message       the message to publish
     * @param pubSubService the pub-sub service mediator
     */
    public void publish(Message message, PubSubService pubSubService) {
        pubSubService.addMessageToQueue(message);
    }
}


package com.limechain.rpc.pubsub.subscriber;

import com.limechain.rpc.pubsub.PubSubService;
import com.limechain.rpc.pubsub.Topic;
import org.springframework.web.socket.WebSocketSession;

public class SubscriberChannel extends AbstractSubscriberChannel {
    public SubscriberChannel(Topic topic) {
        super(topic);
    }

    // Add subscriber with PubSubService for a topic
    public void addSubscriber(WebSocketSession session) {
        // We shouldn't allow client to subscribe more than once for the same event.
        if (this.getSessions()
                .stream()
                .anyMatch(s -> s.getId().equals(session.getId()))) {
            return;
        }
        this.getSessions().add(session);
    }

    // Unsubscribe subscriber with PubSubService for a topic
    public void removeSubscriber(WebSocketSession session) {
        this.getSessions().removeIf(s -> s.getId().equals(session.getId()));
    }

    // Request specifically for messages related to topic from PubSubService
    public void getMessagesForSubscriberOfTopic(PubSubService pubSubService) {
        pubSubService.getMessagesForSubscriberOfTopic(this);
    }
}

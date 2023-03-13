package com.limechain.rpc.pubsub.subscriber;

import com.limechain.rpc.pubsub.PubSubService;
import com.limechain.rpc.pubsub.Topic;
import org.springframework.web.socket.WebSocketSession;

public class SubscriberImpl extends Subscriber {
    //Add subscriber with PubSubService for a topic
    public void addSubscriber(Topic topic, WebSocketSession session) {
        this.getSessions().add(session);
    }

    //Unsubscribe subscriber with PubSubService for a topic
    public void unsubscribe(Topic topic, WebSocketSession session) {
        this.getSessions().removeIf(s -> s.getId().equals(session.getId()));
    }

    //Request specifically for messages related to topic from PubSubService
    public void getMessagesForSubscriberOfTopic(Topic topic, PubSubService pubSubService) {
        pubSubService.getMessagesForSubscriberOfTopic(topic, this);

    }
}

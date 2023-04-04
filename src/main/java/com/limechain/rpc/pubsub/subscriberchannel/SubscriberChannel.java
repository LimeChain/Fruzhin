package com.limechain.rpc.pubsub.subscriberchannel;

import com.limechain.rpc.pubsub.Message;
import com.limechain.rpc.pubsub.Topic;
import org.springframework.web.socket.WebSocketSession;

public class SubscriberChannel extends AbstractSubscriberChannel {
    public SubscriberChannel(Topic topic) {
        super(topic);
    }

    public void addSubscriber(WebSocketSession session) {
        // We shouldn't allow client to subscribe more than once for the same event.
        if (this.getSubscribers()
                .stream()
                .anyMatch(s -> s.getId().equals(session.getId()))) {
            return;
        }
        this.getSubscribers().add(session);
    }

    public void removeSubscriber(WebSocketSession session) {
        this.getSubscribers().removeIf(s -> s.getId().equals(session.getId()));
    }

    @Override
    public void addMessage(Message message) {
        this.getPendingMessages().add(message);
    }
}

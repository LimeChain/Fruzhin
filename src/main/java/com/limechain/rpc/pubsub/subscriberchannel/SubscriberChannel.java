package com.limechain.rpc.pubsub.subscriberchannel;

import com.limechain.rpc.pubsub.Message;
import com.limechain.rpc.pubsub.Topic;
import org.springframework.web.socket.WebSocketSession;

public class SubscriberChannel extends AbstractSubscriberChannel {
    public SubscriberChannel(Topic topic) {
        super(topic);
    }

    public Subscriber addSubscriber(WebSocketSession session) {
        // We shouldn't allow client to subscribe more than once for the same event.
        if (this.getSubscribers()
                .values()
                .stream()
                .anyMatch(s -> s.getSession().getId().equals(session.getId()))) {
            return null;
        }

        Subscriber subscriber = new Subscriber(generateSubscriptionId(), session);
        this.getSubscribers().put(subscriber.getSubscriptionId(), subscriber);
        return subscriber;
    }

    public void removeSubscriber(final String subId) {
        this.getSubscribers().remove(subId);
    }

    @Override
    public void addMessage(Message message) {
        this.getPendingMessages().add(message);
    }
}

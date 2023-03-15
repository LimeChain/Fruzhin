package com.limechain.rpc.pubsub;

import com.limechain.rpc.pubsub.subscriber.AbstractSubscriberChannel;
import com.limechain.rpc.pubsub.subscriber.SubscriberChannel;
import lombok.extern.java.Log;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;

@Log
public class PubSubService {
    private static final PubSubService INSTANCE = new PubSubService();

    // Keeps map of subscriber topic wise, using map to prevent duplicates
    private final Map<Topic, AbstractSubscriberChannel> subscribersTopicMap = new HashMap<>() {{
        // TODO: Instantiate more subscriber channels in the future
        put(Topic.UNSTABLE_FOLLOW, new SubscriberChannel(Topic.UNSTABLE_FOLLOW));
        put(Topic.UNSTABLE_TRANSACTION_WATCH, new SubscriberChannel(Topic.UNSTABLE_TRANSACTION_WATCH));
    }};

    // Holds messages published by publishers
    private final Queue<Message> messagesQueue = new LinkedList<>();

    // Private constructor to avoid client applications using the constructor
    private PubSubService() {
    }

    public static PubSubService getInstance() {
        return INSTANCE;
    }

    // Adds message sent by publisher to queue
    public void addMessageToQueue(Message message) {
        messagesQueue.add(message);
    }

    // Add a new Subscriber for a topic
    public void addSubscriber(Topic topic, WebSocketSession session) {
        if (subscribersTopicMap.containsKey(topic)) {
            AbstractSubscriberChannel subscriberChannel = subscribersTopicMap.get(topic);
            subscriberChannel.addSubscriber(session);
            return;
        }

        log.log(Level.WARNING, "Didn't subscribe session to topic. Topic doesn't exist");
    }

    // Remove an existing subscriber for a topic
    public void removeSubscriber(Topic topic, String sessionId) {
        if (subscribersTopicMap.containsKey(topic)) {
            AbstractSubscriberChannel subscriberChannel = subscribersTopicMap.get(topic);
            subscriberChannel
                    .getSessions()
                    .stream()
                    .filter(s -> s.getId().equals(sessionId))
                    .findFirst()
                    .ifPresent(subscriberChannel::removeSubscriber);

        }
    }

    // Broadcast new messages added in queue to All subscribers of the topic.
    // messagesQueue will be empty after broadcasting.
    public void broadcast() {
        if (messagesQueue.isEmpty()) {
            log.log(Level.FINE, "No messages from publishers to broadcast to subscribers");
        } else {
            while (!messagesQueue.isEmpty()) {
                Message message = messagesQueue.remove();
                String topic = message.topic();

                AbstractSubscriberChannel subscriberChannel = subscribersTopicMap.get(Topic.fromString(topic));
                // If subscriberChannel is null, the message will get lost
                if (subscriberChannel != null) {
                    subscriberChannel.getPendingMessages().add(message);
                }
            }
        }
    }

    // Iterate over all subscriber channels and send all pending messages to each subscriber
    public void notifySubscribers() {
        for (Map.Entry<Topic, AbstractSubscriberChannel> set : subscribersTopicMap.entrySet()) {
            try {
                set.getValue().notifySubscribers();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    // Sends messages about a topic for subscriber at any point
    public void getMessagesForSubscriberOfTopic(AbstractSubscriberChannel subscriber) {
        if (messagesQueue.isEmpty()) {
            log.log(Level.FINE, "No messages from publishers to display");
            return;
        }

        while (!messagesQueue.isEmpty()) {
            Message message = messagesQueue.remove();
            if (message.topic().equalsIgnoreCase(subscriber.getTopic().getValue())) {
                if (subscribersTopicMap.get(subscriber.getTopic()).equals(subscriber)) {
                    // Add broadcast message to subscriber message queue
                    subscriber.getPendingMessages().add(message);
                }
            }
        }
    }
}

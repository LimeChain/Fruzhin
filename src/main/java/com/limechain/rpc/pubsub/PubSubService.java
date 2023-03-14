package com.limechain.rpc.pubsub;

import com.limechain.rpc.pubsub.subscriber.AbstractSubscriberChannel;
import com.limechain.rpc.pubsub.subscriber.SubscriberChannel;
import lombok.extern.java.Log;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;

@Log
public class PubSubService {
    private static final PubSubService INSTANCE = new PubSubService();

    // Keeps map of subscriber topic wise, using map to prevent duplicates
    private final Map<Topic, AbstractSubscriberChannel> subscribersTopicMap = new HashMap<>() {{
        // TODO: Instantiate more subscriber channels in the future
        put(Topic.UNSTABLE_FOLLOW, new SubscriberChannel());
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
        // TODO: We shouldn't allow client to subscribe more than once for the same event.
        // We can do that by checking if the sessionId already exists in the subscriber list
        if (subscribersTopicMap.containsKey(topic)) {
            subscribersTopicMap.get(topic).addSubscriber(topic, session);
        }
    }

    // Remove an existing subscriber for a topic
    public void removeSubscriber(Topic topic, String sessionId) {
        if (subscribersTopicMap.containsKey(topic)) {
            AbstractSubscriberChannel subscriber = subscribersTopicMap.get(topic);
            subscriber.getSessions()
                    .stream()
                    .filter(s -> s.getId().equals(sessionId))
                    .findFirst()
                    .ifPresent(session -> subscriber.removeSubscriber(topic, session));

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

                AbstractSubscriberChannel subscriber = subscribersTopicMap.get(Topic.fromString(topic));
                List<Message> subscriberMessages = subscriber.getPendingMessages();
                subscriberMessages.add(message);
                subscriber.setPendingMessages(subscriberMessages);
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
    public void getMessagesForSubscriberOfTopic(Topic topic, AbstractSubscriberChannel subscriber) {
        if (messagesQueue.isEmpty()) {
            log.log(Level.FINE, "No messages from publishers to display");
            return;
        }

        while (!messagesQueue.isEmpty()) {
            Message message = messagesQueue.remove();
            if (message.topic().equalsIgnoreCase(topic.getValue())) {
                if (subscribersTopicMap.get(topic).equals(subscriber)) {
                    // Add broadcast message to subscriber message queue
                    List<Message> subscriberMessages = subscriber.getPendingMessages();
                    subscriberMessages.add(message);
                    subscriber.setPendingMessages(subscriberMessages);
                }
            }
        }
    }

}

package com.limechain.rpc.pubsub;

import com.limechain.rpc.pubsub.subscriberchannel.AbstractSubscriberChannel;
import com.limechain.rpc.pubsub.subscriberchannel.SubscriberChannel;
import lombok.extern.java.Log;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;

/**
 * A singleton mediator class standing between {@link com.limechain.rpc.pubsub.publisher.Publisher}
 * and {@link AbstractSubscriberChannel} which accepts messages from the formers and sends them at some point
 * to the latter.
 * <p>
 * <b>IMPORTANT: This class is a singleton as both http and ws Spring apps
 * use it and we want to always have a single reference.
 * Otherwise each Spring app will have it's own instance of this
 * service which will lead to undocumented runtime behaviour</b>
 */
@Log
public class PubSubService {
    private static final PubSubService INSTANCE = new PubSubService();

    /**
     * Keeps map of subscriber topic wise, using map to prevent duplicates
     */
    private final Map<Topic, AbstractSubscriberChannel> subscribersTopicMap = new HashMap<>() {{
        // TODO: Instantiate more subscriber channels in the future
        put(Topic.UNSTABLE_FOLLOW, new SubscriberChannel(Topic.UNSTABLE_FOLLOW));
        put(Topic.UNSTABLE_TRANSACTION_WATCH, new SubscriberChannel(Topic.UNSTABLE_TRANSACTION_WATCH));
    }};

    /**
     * Holds messages published by publishers which will be broadcast to each subscriber channel at some point
     */
    private final Queue<Message> messagesQueue = new LinkedList<>();

    /**
     * Private constructor to avoid client applications using the constructor
     */
    private PubSubService() {
    }

    /**
     * Gets the singleton reference
     */
    public static PubSubService getInstance() {
        return INSTANCE;
    }

    /**
     * Adds message sent by publisher to queue
     *
     * @param message the message to add
     */
    public void addMessageToQueue(Message message) {
        messagesQueue.add(message);
    }

    /**
     * Add a new Subscriber for specific subscriber channel
     *
     * @param topic   the topic of the channel
     * @param session the subscriber to add
     */
    public void addSubscriber(Topic topic, WebSocketSession session) {
        if (subscribersTopicMap.containsKey(topic)) {
            AbstractSubscriberChannel subscriberChannel = subscribersTopicMap.get(topic);
            subscriberChannel.addSubscriber(session);
            return;
        }

        log.log(Level.WARNING, "Didn't subscribe session to topic. Topic doesn't exist: " + topic.getValue());
    }

    /**
     * Remove an existing subscriber from a channel
     *
     * @param topic     the topic of the channel
     * @param sessionId the subscriber id to remove
     */
    public void removeSubscriber(Topic topic, String sessionId) {
        if (subscribersTopicMap.containsKey(topic)) {
            AbstractSubscriberChannel subscriberChannel = subscribersTopicMap.get(topic);
            subscriberChannel
                    .getSubscribers()
                    .stream()
                    .filter(s -> s.getId().equals(sessionId))
                    .findFirst()
                    .ifPresent(subscriberChannel::removeSubscriber);

        }
    }

    /**
     * Broadcast messages from the queue to the corresponding subscriber channel.
     * {@link #messagesQueue} will be empty after broadcasting.
     */
    public void broadcast() {
        while (!messagesQueue.isEmpty()) {
            Message message = messagesQueue.remove();
            String topic = message.topic();

            AbstractSubscriberChannel subscriberChannel = subscribersTopicMap.get(Topic.fromString(topic));
            // If subscriberChannel is null, the message will get lost
            if (subscriberChannel != null) {
                subscriberChannel.addMessage(message);
            }
        }
    }

    /**
     * Iterate over each subscriber channel and send it's pending messages to each subscriber
     */
    public void notifySubscribers() {
        for (Map.Entry<Topic, AbstractSubscriberChannel> set : subscribersTopicMap.entrySet()) {
            try {
                set.getValue().notifySubscribers();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}

package com.limechain.rpc.pubsub;

import com.limechain.exception.NotificationFailedException;
import com.limechain.rpc.exceptions.WsMessageSendException;
import com.limechain.rpc.pubsub.subscriberchannel.AbstractSubscriberChannel;
import com.limechain.rpc.pubsub.subscriberchannel.Subscriber;
import com.limechain.rpc.pubsub.subscriberchannel.SubscriberChannel;
import lombok.extern.java.Log;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;

/**
 * A singleton mediator class standing between {@link com.limechain.rpc.pubsub.publisher.Publisher}
 * and {@link AbstractSubscriberChannel} which accepts messages from the formers and sends them at some point
 * to the latter.
 */
@Log
public class PubSubService {
    private static final PubSubService INSTANCE = new PubSubService();

    /**
     * Keeps map of subscriber topic wise, using map to prevent duplicates
     */
    private final Map<Topic, AbstractSubscriberChannel> subscribersTopicMap = subscribersTopicMap();

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
    public Subscriber addSubscriber(Topic topic, WebSocketSession session) {
        if (subscribersTopicMap.containsKey(topic)) {
            AbstractSubscriberChannel subscriberChannel = subscribersTopicMap.get(topic);
            return subscriberChannel.addSubscriber(session);
        }

        log.log(Level.WARNING, "Didn't subscribe session to topic. Topic doesn't exist: " + topic.getValue());
        return null;
    }

    /**
     * Remove an existing subscriber from a channel
     *
     * @param topic          the topic of the channel
     * @param subscriptionId the subscriber id to remove
     */
    public boolean removeSubscriber(Topic topic, String subscriptionId) {
        if (subscribersTopicMap.containsKey(topic)) {
            AbstractSubscriberChannel subscriberChannel = subscribersTopicMap.get(topic);
            subscriberChannel.removeSubscriber(subscriptionId);
            return true;
        }
        return false;
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
                throw new NotificationFailedException(e);
            }
        }
    }

    private Map<Topic, AbstractSubscriberChannel> subscribersTopicMap() {
        // TODO: Instantiate more subscriber channels in the future if needed
        var map = new EnumMap<Topic, AbstractSubscriberChannel>(Topic.class);
        for (Topic topic : Topic.values()) {
            map.put(topic, new SubscriberChannel(topic));
        }
        return map;
    }

    public void sendResultMessage(final WebSocketSession session, final String message) {
        try {
            session.sendMessage(new TextMessage(parseResultMessage(message)));
        } catch (Exception e) {
            throw new WsMessageSendException("Failed to send message to ws client");
        }
    }

    private String parseResultMessage(final String result) {
        return "{\"id\":1,\"jsonrpc\":\"2.0\",\"result\":\"" + result + "\"}";
    }
}

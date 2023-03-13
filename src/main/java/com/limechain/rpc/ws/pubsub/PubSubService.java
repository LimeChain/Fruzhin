package com.limechain.rpc.ws.pubsub;

import com.limechain.rpc.ws.pubsub.subscriber.Subscriber;
import com.limechain.rpc.ws.pubsub.subscriber.SubscriberImpl;
import lombok.extern.java.Log;
import org.springframework.scheduling.annotation.Scheduled;
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
    //Keeps set of subscriber topic wise, using set to prevent duplicates
    Map<Topic, Subscriber> subscribersTopicMap = new HashMap<>() {{
        // TODO: Instantiate more subscriber channels in the future
        put(Topic.UNSTABLE_FOLLOW, new SubscriberImpl());
    }};

    //Holds messages published by publishers
    Queue<Message> messagesQueue = new LinkedList<>();

    //Adds message sent by publisher to queue
    public void addMessageToQueue(Message message) {
        messagesQueue.add(message);
    }

    //Add a new Subscriber for a topic
    public void addSubscriber(Topic topic, WebSocketSession session) {
        //TODO: We shouldn't allow client to subscribe more than once for the same event.
        // We can do that by checking if the sessionId already exists in the subscriber list
        if (subscribersTopicMap.containsKey(topic)) {
            subscribersTopicMap.get(topic).addSubscriber(topic, session);
        }
    }

    //Remove an existing subscriber for a topic
    public void removeSubscriber(Topic topic, String sessionId) {
        if (subscribersTopicMap.containsKey(topic)) {
            Subscriber subscriber = subscribersTopicMap.get(topic);
            subscriber.getSessions()
                    .stream()
                    .filter(s -> s.getId().equals(sessionId))
                    .findFirst()
                    .ifPresent(session -> subscriber.unsubscribe(topic, session));

        }
    }

    // Broadcast new messages added in queue to All subscribers of the topic.
    // messagesQueue will be empty after broadcasting.
    @Scheduled(fixedDelay = 2000)
    public void broadcast() {
        if (messagesQueue.isEmpty()) {
            log.log(Level.INFO, "No messages from publishers to broadcast to subscribers");
        } else {
            while (!messagesQueue.isEmpty()) {
                Message message = messagesQueue.remove();
                String topic = message.getTopic();

                Subscriber subscriber = subscribersTopicMap.get(Topic.fromString(topic));
                List<Message> subscriberMessages = subscriber.getPendingMessages();
                subscriberMessages.add(message);
                subscriber.setPendingMessages(subscriberMessages);
            }
        }
    }

    // Iterate over all subscriber channels and send all pending messages to each subscriber
    @Scheduled(fixedDelay = 5000)
    public void notifySubscribers() throws IOException {
        for (Map.Entry<Topic, Subscriber> set : subscribersTopicMap.entrySet()) {
            set.getValue().notifySubscribers();
        }
    }


    // Sends messages about a topic for subscriber at any point
    public void getMessagesForSubscriberOfTopic(Topic topic, Subscriber subscriber) {
        if (messagesQueue.isEmpty()) {
            log.log(Level.INFO, "No messages from publishers to display");
            return;
        }

        while (!messagesQueue.isEmpty()) {
            Message message = messagesQueue.remove();
            if (message.getTopic().equalsIgnoreCase(topic.getValue())) {
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

package com.limechain.rpc.pubsub.subscriber;

import com.limechain.rpc.pubsub.Message;
import com.limechain.rpc.pubsub.PubSubService;
import com.limechain.rpc.pubsub.Topic;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

// Subscribers are the entities who subscribe to messages on a topic.

@Getter
@Setter
@Log
public abstract class AbstractSubscriberChannel {
    private final List<WebSocketSession> sessions = new ArrayList<>();
    // Store all messages received by the subscriber
    private List<Message> pendingMessages = new ArrayList<>();

    // Add subscriber with PubSubService for a topic
    public abstract void addSubscriber(Topic topic, WebSocketSession session);

    // Unsubscribe subscriber with PubSubService for a topic
    public abstract void removeSubscriber(Topic topic, WebSocketSession session);

    // Request specifically for messages related to topic from PubSubService
    public abstract void getMessagesForSubscriberOfTopic(Topic topic, PubSubService pubSubService);

    // Print all messages received by the subscriber
    public synchronized void notifySubscribers() throws IOException {
        log.log(Level.FINE, "Sending messages to subscribers...");
        // What happens if PubSubService tries to add new messages while we're in the for loop?
        // Option 1. Messages get added normally (highly unlikely since there's no lock on subscriberMessages)
        // Option 2. Messages get added and processed on the next run of printMessages
        // Option 3. Messages get added but overwritten by new ArrayList<>() at the end of this function
        // Option 4. Option 2 and 3 depending on the timing
        for (Message message : pendingMessages) {
            TextMessage wsMessage = new TextMessage(message.payload().getBytes());
            log.log(Level.FINE,
                    "Notifying " + sessions.size() + " subscribers about message topic -> " + message.topic() +
                            " : " +
                            message.payload());
            for (WebSocketSession session : sessions) {
                session.sendMessage(wsMessage);
            }
        }
        // Empty the pending messages
        pendingMessages = new ArrayList<>();
    }
}


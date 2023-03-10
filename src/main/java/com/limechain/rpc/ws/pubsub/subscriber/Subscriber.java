package com.limechain.rpc.ws.pubsub.subscriber;

import com.limechain.rpc.ws.pubsub.Message;
import com.limechain.rpc.ws.pubsub.PubSubService;
import com.limechain.rpc.ws.pubsub.Topic;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// Subscribers are the entities who subscribe to messages on a topic.

@Getter
@Setter
public abstract class Subscriber {
    private final List<WebSocketSession> sessions = new ArrayList<>();
    //store all messages received by the subscriber
    private List<Message> pendingMessages = new ArrayList<>();

    //Add subscriber with PubSubService for a topic
    public abstract void addSubscriber(Topic topic, WebSocketSession session);

    //Unsubscribe subscriber with PubSubService for a topic
    public abstract void unsubscribe(Topic topic, WebSocketSession session);

    //Request specifically for messages related to topic from PubSubService
    public abstract void getMessagesForSubscriberOfTopic(Topic topic, PubSubService pubSubService);

    //Print all messages received by the subscriber
    public void notifySubscribers() throws IOException {
        System.out.println("Sending messages to subscribers...");
        // What happens if PubSubService tries to add new messages while we're in the for loop?
        // Option 1. Messages get added normally (highly unlikely since there's no lock on subscriberMessages)
        // Option 2. Messages get added and processed on the next run of printMessages
        // Option 3. Messages get added but overwritten by new ArrayList<>() at the end of this function
        // Option 4. Option 2 and 3 depending on the timing
        for (Message message : pendingMessages) {
            TextMessage wsMessage = new TextMessage(message.getPayload().getBytes());
            System.out.println(
                    "Notifying " + sessions.size() + " subscribers about message topic -> " + message.getTopic() +
                            " : " +
                            message.getPayload());
            for (WebSocketSession session : sessions) {
                session.sendMessage(wsMessage);
            }
        }
        // Empty the pending messages
        pendingMessages = new ArrayList<>();
    }
}


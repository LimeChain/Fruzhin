package com.limechain.rpc.pubsub;

import com.limechain.rpc.pubsub.subscriberchannel.AbstractSubscriberChannel;
import com.limechain.rpc.pubsub.subscriberchannel.Subscriber;
import com.limechain.rpc.pubsub.subscriberchannel.SubscriberChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class PubSubServiceTest {
    private final PubSubService service = PubSubService.getInstance();

    // Setting private fields. Not a good idea in general
    private void setPrivateField(String fieldName, Object value)
            throws NoSuchFieldException, IllegalAccessException {
        Field privateField = PubSubService.class.getDeclaredField(fieldName);
        privateField.setAccessible(true);

        privateField.set(service, value);
    }

    // Accessing private fields. Not a good idea in general
    private Object getPrivateField(String fieldName)
            throws NoSuchFieldException, IllegalAccessException {
        Field privateField = PubSubService.class.getDeclaredField(fieldName);
        privateField.setAccessible(true);

        return privateField.get(service);
    }

    @BeforeEach
    public void setup() throws NoSuchFieldException, IllegalAccessException {
        // Reset state of singleton manually before each state
        // Not the best approach but can't reset it using new PubSubService() because of private constructor
        setPrivateField("subscribersTopicMap", new HashMap<>() {{
            // TODO: Instantiate more subscriber channels in the future if needed
            for (Topic value : Topic.values()) {
                put(value, new SubscriberChannel(value));
            }
        }});

        setPrivateField("messagesQueue", new LinkedList<>());
    }

    @Test
    void GetInstance_returns_sameReference() {
        PubSubService reference1 = PubSubService.getInstance();
        PubSubService reference2 = PubSubService.getInstance();

        assertEquals(reference1, reference2);
    }

    @Test
    void AddMessageToQueue_addsMessage() throws NoSuchFieldException, IllegalAccessException {
        Message message = new Message(Topic.UNSTABLE_TRANSACTION_WATCH, "test payload");

        // How to proceed? Can't verify since messagesQueue is private

        service.addMessageToQueue(message);

        Queue<Message> messageQueue = (Queue<Message>) getPrivateField("messagesQueue");

        assertEquals(1, messageQueue.size());
        assertEquals(message, messageQueue.remove());
    }

    @Test
    void AddSubscriber_callsChannelAddSubscriber_whenTopicExists()
            throws NoSuchFieldException, IllegalAccessException {
        SubscriberChannel channel = mock(SubscriberChannel.class);
        WebSocketSession session = mock(WebSocketSession.class);

        Map<Topic, AbstractSubscriberChannel> map =
                (Map<Topic, AbstractSubscriberChannel>) getPrivateField("subscribersTopicMap");

        // Overwrite channel with mocked channel
        map.put(Topic.UNSTABLE_FOLLOW, channel);

        service.addSubscriber(Topic.UNSTABLE_FOLLOW, session);

        verify(channel, times(1)).addSubscriber(session);
    }

    @Test
    void AddSubscriber_doesNotCallChannelAddSubscriber_whenTopicDoesNotExist()
            throws NoSuchFieldException, IllegalAccessException {
        SubscriberChannel channel = mock(SubscriberChannel.class);
        WebSocketSession session = mock(WebSocketSession.class);

        // Simulate that we don't have a channel for a topic
        HashMap<Object, Object> map = new HashMap<>() {{
            put(Topic.UNSTABLE_FOLLOW, channel);
        }};
        setPrivateField("subscribersTopicMap", map);

        service.addSubscriber(Topic.UNSTABLE_TRANSACTION_WATCH, session);

        verify(channel, times(0)).addSubscriber(session);
    }

    @Test
    void RemoveSubscriber_callsChannelRemoveSubscriber_whenSessionExist()
            throws NoSuchFieldException, IllegalAccessException {
        SubscriberChannel channel = mock(SubscriberChannel.class);
        WebSocketSession session = mock(WebSocketSession.class);

        String subscriptionId = "0";
        doReturn(new HashMap<String, Subscriber>() {{
            put(subscriptionId, new Subscriber(subscriptionId, session));
        }}).when(channel).getSubscribers();

        Map<Topic, AbstractSubscriberChannel> map =
                (Map<Topic, AbstractSubscriberChannel>) getPrivateField("subscribersTopicMap");

        // Overwrite channel with mocked channel
        map.put(Topic.UNSTABLE_FOLLOW, channel);

        service.removeSubscriber(Topic.UNSTABLE_FOLLOW, subscriptionId);

        verify(channel, times(1)).removeSubscriber(subscriptionId);
    }

    @Test
    void RemoveSubscriber_doesNotCallChannelRemoveSubscriber_whenTopicDoesNotExist()
            throws NoSuchFieldException, IllegalAccessException {
        SubscriberChannel channel = mock(SubscriberChannel.class);
        WebSocketSession session = mock(WebSocketSession.class);

        String subscriptionId = "0";

        // Simulate that we don't have a channel for a topic
        HashMap<Object, Object> map = new HashMap<>() {{
            put(Topic.UNSTABLE_FOLLOW, channel);
        }};
        setPrivateField("subscribersTopicMap", map);

        service.removeSubscriber(Topic.UNSTABLE_TRANSACTION_WATCH, session.getId());

        verify(channel, times(0)).removeSubscriber(subscriptionId);
    }

    @Test
    void broadcast_emptiesMessageQueue_whenCalled() throws NoSuchFieldException, IllegalAccessException {
        Message message1 = new Message(Topic.UNSTABLE_FOLLOW, "message1");
        Message message2 = new Message(Topic.UNSTABLE_FOLLOW, "message2");
        Message message3 = new Message(Topic.UNSTABLE_TRANSACTION_WATCH, "message3");

        service.addMessageToQueue(message1);
        service.addMessageToQueue(message2);
        service.addMessageToQueue(message3);

        Queue<Message> map = (Queue<Message>) getPrivateField("messagesQueue");

        assertEquals(3, map.size());

        service.broadcast();

        assertEquals(0, map.size());
    }

    @Test
    void notifySubscribers_callsNotifySubscribers_forAllChannels()
            throws NoSuchFieldException, IllegalAccessException, IOException {
        SubscriberChannel channel1 = mock(SubscriberChannel.class);
        SubscriberChannel channel2 = mock(SubscriberChannel.class);

        setPrivateField("subscribersTopicMap", new HashMap<>() {{
            put(Topic.UNSTABLE_FOLLOW, channel1);
            put(Topic.UNSTABLE_TRANSACTION_WATCH, channel2);
        }});

        service.notifySubscribers();

        verify(channel1, times(1)).notifySubscribers();
        verify(channel2, times(1)).notifySubscribers();
    }

    @Test
    void notifySubscribers_throwsRuntimeException_whenNotifySubscribersFails()
            throws NoSuchFieldException, IllegalAccessException, IOException {
        SubscriberChannel channel1 = mock(SubscriberChannel.class);
        doThrow(new IOException()).when(channel1).notifySubscribers();
        setPrivateField("subscribersTopicMap", new HashMap<>() {{
            put(Topic.UNSTABLE_FOLLOW, channel1);
        }});

        assertThrows(RuntimeException.class, () -> service.notifySubscribers());
    }

}
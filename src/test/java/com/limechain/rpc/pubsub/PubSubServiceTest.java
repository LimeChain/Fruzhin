package com.limechain.rpc.pubsub;

import com.limechain.rpc.pubsub.subscriberchannel.AbstractSubscriberChannel;
import com.limechain.rpc.pubsub.subscriberchannel.SubscriberChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
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
    // Accessing private fields. Not a good idea in general
    private static Object getPrivateField(PubSubService service, String fieldName)
            throws NoSuchFieldException, IllegalAccessException {
        Field privateField = PubSubService.class.getDeclaredField(fieldName);
        privateField.setAccessible(true);

        return privateField.get(service);
    }

    // Setting private fields. Not a good idea in general
    private static void setPrivateField(PubSubService service, String fieldName, Object value)
            throws NoSuchFieldException, IllegalAccessException {
        Field privateField = PubSubService.class.getDeclaredField(fieldName);
        privateField.setAccessible(true);

        privateField.set(service, value);
    }

    @BeforeEach
    public void setup() throws NoSuchFieldException, IllegalAccessException {
        // Reset state of singleton manually before each state
        // Not the best approach but can't reset it using new PubSubService() because of private constructor
        setPrivateField(PubSubService.getInstance(), "subscribersTopicMap", new HashMap<>() {{
            // TODO: Instantiate more subscriber channels in the future
            put(Topic.UNSTABLE_FOLLOW, new SubscriberChannel(Topic.UNSTABLE_FOLLOW));
            put(Topic.UNSTABLE_TRANSACTION_WATCH, new SubscriberChannel(Topic.UNSTABLE_TRANSACTION_WATCH));
        }});

        setPrivateField(PubSubService.getInstance(), "messagesQueue", new LinkedList<>());
    }

    @Test
    public void GetInstance_returns_sameReference() {
        PubSubService reference1 = PubSubService.getInstance();
        PubSubService reference2 = PubSubService.getInstance();

        assertEquals(reference1, reference2);
    }

    @Test
    public void AddMessageToQueue_addsMessage() throws NoSuchFieldException, IllegalAccessException {
        PubSubService service = PubSubService.getInstance();
        Message message = new Message(Topic.UNSTABLE_TRANSACTION_WATCH.getValue(), "test payload");

        // How to proceed? Can't verify since messagesQueue is private

        service.addMessageToQueue(message);

        Queue<Message> messageQueue = (Queue<Message>) getPrivateField(service, "messagesQueue");

        assertEquals(1, messageQueue.size());
        assertEquals(message, messageQueue.remove());
    }

    @Test
    public void AddSubscriber_callsChannelAddSubscriber_whenTopicExists()
            throws NoSuchFieldException, IllegalAccessException {
        PubSubService service = PubSubService.getInstance();
        SubscriberChannel channel = mock(SubscriberChannel.class);
        WebSocketSession session = mock(WebSocketSession.class);

        Map<Topic, AbstractSubscriberChannel> map =
                (Map<Topic, AbstractSubscriberChannel>) getPrivateField(service, "subscribersTopicMap");

        // Overwrite channel with mocked channel
        map.put(Topic.UNSTABLE_FOLLOW, channel);

        service.addSubscriber(Topic.UNSTABLE_FOLLOW, session);

        verify(channel, times(1)).addSubscriber(session);
    }

    @Test
    public void AddSubscriber_doesNotCallChannelAddSubscriber_whenTopicDoesNotExist()
            throws NoSuchFieldException, IllegalAccessException {
        PubSubService service = PubSubService.getInstance();
        SubscriberChannel channel = mock(SubscriberChannel.class);
        WebSocketSession session = mock(WebSocketSession.class);


        // Simulate that we don't have a channel for a topic
        HashMap<Object, Object> map = new HashMap<>() {{
            put(Topic.UNSTABLE_FOLLOW, channel);
        }};
        setPrivateField(service, "subscribersTopicMap", map);

        service.addSubscriber(Topic.UNSTABLE_TRANSACTION_WATCH, session);

        verify(channel, times(0)).addSubscriber(session);
    }

    @Test
    public void RemoveSubscriber_callsChannelRemoveSubscriber_whenSessionExist()
            throws NoSuchFieldException, IllegalAccessException {
        PubSubService service = PubSubService.getInstance();
        SubscriberChannel channel = mock(SubscriberChannel.class);
        WebSocketSession session = mock(WebSocketSession.class);

        doReturn("1").when(session).getId();
        doReturn(new ArrayList<>() {{
            add(session);
        }}).when(channel).getSubscribers();

        Map<Topic, AbstractSubscriberChannel> map =
                (Map<Topic, AbstractSubscriberChannel>) getPrivateField(service, "subscribersTopicMap");

        // Overwrite channel with mocked channel
        map.put(Topic.UNSTABLE_FOLLOW, channel);

        service.removeSubscriber(Topic.UNSTABLE_FOLLOW, session.getId());

        verify(channel, times(1)).removeSubscriber(session);
    }

    @Test
    public void RemoveSubscriber_doesNotCallChannelRemoveSubscriber_whenSessionDoesNotExist()
            throws NoSuchFieldException, IllegalAccessException {
        PubSubService service = PubSubService.getInstance();
        SubscriberChannel channel = mock(SubscriberChannel.class);
        WebSocketSession session = mock(WebSocketSession.class);

        doReturn("1").when(session).getId();
        doReturn(new ArrayList<>()).when(channel).getSubscribers();

        Map<Topic, AbstractSubscriberChannel> map =
                (Map<Topic, AbstractSubscriberChannel>) getPrivateField(service, "subscribersTopicMap");

        // Overwrite channel with mocked channel
        map.put(Topic.UNSTABLE_FOLLOW, channel);

        service.removeSubscriber(Topic.UNSTABLE_FOLLOW, session.getId());

        verify(channel, times(0)).removeSubscriber(session);
    }

    @Test
    public void RemoveSubscriber_doesNotCallChannelRemoveSubscriber_whenTopicDoesNotExist()
            throws NoSuchFieldException, IllegalAccessException {
        PubSubService service = PubSubService.getInstance();
        SubscriberChannel channel = mock(SubscriberChannel.class);
        WebSocketSession session = mock(WebSocketSession.class);

        doReturn("1").when(session).getId();

        // Simulate that we don't have a channel for a topic
        HashMap<Object, Object> map = new HashMap<>() {{
            put(Topic.UNSTABLE_FOLLOW, channel);
        }};
        setPrivateField(service, "subscribersTopicMap", map);

        service.removeSubscriber(Topic.UNSTABLE_TRANSACTION_WATCH, session.getId());

        verify(channel, times(0)).removeSubscriber(session);
    }

    @Test
    public void broadcast_emptiesMessageQueue_whenCalled() throws NoSuchFieldException, IllegalAccessException {
        PubSubService service = PubSubService.getInstance();
        Message message1 = new Message(Topic.UNSTABLE_FOLLOW.getValue(), "message1");
        Message message2 = new Message(Topic.UNSTABLE_FOLLOW.getValue(), "message2");
        Message message3 = new Message(Topic.UNSTABLE_TRANSACTION_WATCH.getValue(), "message3");

        service.addMessageToQueue(message1);
        service.addMessageToQueue(message2);
        service.addMessageToQueue(message3);

        Queue<Message> map = (Queue<Message>) getPrivateField(service, "messagesQueue");

        assertEquals(3, map.size());

        service.broadcast();

        assertEquals(0, map.size());
    }


    @Test
    public void notifySubscribers_callsNotifySubscribers_forAllChannels()
            throws NoSuchFieldException, IllegalAccessException, IOException {
        PubSubService service = PubSubService.getInstance();
        SubscriberChannel channel1 = mock(SubscriberChannel.class);
        SubscriberChannel channel2 = mock(SubscriberChannel.class);

        setPrivateField(service, "subscribersTopicMap", new HashMap<>() {{
            put(Topic.UNSTABLE_FOLLOW, channel1);
            put(Topic.UNSTABLE_TRANSACTION_WATCH, channel2);
        }});

        service.notifySubscribers();

        verify(channel1, times(1)).notifySubscribers();
        verify(channel2, times(1)).notifySubscribers();
    }

    @Test
    public void notifySubscribers_throwsRuntimeException_whenNotifySubscribersFails()
            throws NoSuchFieldException, IllegalAccessException, IOException {
        PubSubService service = PubSubService.getInstance();
        SubscriberChannel channel1 = mock(SubscriberChannel.class);
        doThrow(new IOException()).when(channel1).notifySubscribers();
        setPrivateField(service, "subscribersTopicMap", new HashMap<>() {{
            put(Topic.UNSTABLE_FOLLOW, channel1);
        }});

        assertThrows(RuntimeException.class, () -> service.notifySubscribers());
    }

}
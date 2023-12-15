package com.limechain.rpc.pubsub.subscriberchannel;

import com.limechain.rpc.pubsub.Message;
import com.limechain.rpc.pubsub.Topic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class AbstractSubscriberChannelTest {
    private final Topic topic = Topic.UNSTABLE_FOLLOW;
    private SubscriberChannel channel;

    @BeforeEach
    public void setup() {
        this.channel = new SubscriberChannel(topic);
    }

    @Test
    void constructor_setsTopic() {
        assertEquals(Topic.UNSTABLE_TRANSACTION_WATCH,
                new SubscriberChannel(Topic.UNSTABLE_TRANSACTION_WATCH).getTopic());
    }

    @Test
    void notifySubscribers_callsSessionSendMessage_forEveryMessage() throws IOException {
        Message message1 = new Message(topic.getValue(), "message1");
        Message message2 = new Message(topic.getValue(), "message2");

        WebSocketSession session1 = mock(WebSocketSession.class);
        WebSocketSession session2 = mock(WebSocketSession.class);
        doReturn("1").when(session1).getId();
        doReturn("2").when(session2).getId();

        this.channel.addMessage(message1);
        this.channel.addMessage(message2);

        this.channel.addSubscriber(session1);
        this.channel.addSubscriber(session2);

        this.channel.notifySubscribers();

        verify(session1, times(2)).sendMessage(any());
        verify(session2, times(2)).sendMessage(any());

        assertEquals(0, this.channel.getPendingMessages().size());
    }

}
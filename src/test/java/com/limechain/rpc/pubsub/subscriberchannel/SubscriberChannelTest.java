package com.limechain.rpc.pubsub.subscriberchannel;

import com.limechain.rpc.pubsub.Topic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SubscriberChannelTest {

    private SubscriberChannel channel;

    @BeforeEach
    public void setup() {
        this.channel = new SubscriberChannel(Topic.UNSTABLE_FOLLOW);
    }

    @Test
    void removeSubscriber_removesCorrectSubscriber() {
        WebSocketSession session1 = mock(WebSocketSession.class);
        WebSocketSession session2 = mock(WebSocketSession.class);
        WebSocketSession session3 = mock(WebSocketSession.class);

        when(session1.getId()).thenReturn("1").thenReturn("1");
        when(session2.getId()).thenReturn("2").thenReturn("2");
        when(session3.getId()).thenReturn("3").thenReturn("3");

        Subscriber sub1 = this.channel.addSubscriber(session1);
        Subscriber sub2 = this.channel.addSubscriber(session2);
        Subscriber sub3 = this.channel.addSubscriber(session3);
        assertEquals(3, this.channel.getSubscribers().size());

        this.channel.removeSubscriber(sub2.getSubscriptionId());

        assertTrue(this.channel.getSubscribers().containsKey(sub1.getSubscriptionId()));
        assertTrue(this.channel.getSubscribers().containsKey(sub3.getSubscriptionId()));
        assertEquals(2, this.channel.getSubscribers().size());

    }
}
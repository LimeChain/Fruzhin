package com.limechain.rpc.pubsub.publisher;

import com.limechain.rpc.pubsub.Message;
import com.limechain.rpc.pubsub.PubSubService;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class PublisherImplTest {

    @Test
    void publish_callsAddMessageToQueue() {
        PubSubService pubSubService = mock(PubSubService.class);
        Message message = mock(Message.class);

        var publisher = new PublisherImpl();
        publisher.publish(message, pubSubService);

        verify(pubSubService, times(1)).addMessageToQueue(message);
    }
}
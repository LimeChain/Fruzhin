package com.limechain.rpc.pubsub.publisher;

import com.limechain.rpc.pubsub.Message;
import com.limechain.rpc.pubsub.PubSubService;

public interface Publisher {
    void publish(Message message, PubSubService pubSubService);
}

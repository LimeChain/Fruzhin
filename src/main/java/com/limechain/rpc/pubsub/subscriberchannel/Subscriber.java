package com.limechain.rpc.pubsub.subscriberchannel;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.web.socket.WebSocketSession;

@Data
@AllArgsConstructor
public class Subscriber {
    private final String subscriptionId;
    private final WebSocketSession session;
}

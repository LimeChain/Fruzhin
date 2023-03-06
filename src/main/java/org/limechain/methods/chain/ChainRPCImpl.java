package org.limechain.methods.chain;

import io.emeraldpay.polkaj.api.Subscription;
import org.limechain.methods.chain.events.FollowEvent;
import org.limechain.methods.chain.subscriptions.SubscriptionCalls;
import org.limechain.methods.chain.subscriptions.SubscriptionHandlers;
import org.limechain.ws.client.WebSocketClient;
import org.springframework.stereotype.Service;

@Service
public class ChainRPCImpl {
    private final String NodeEndpoint = "";

    private final WebSocketClient wsClient;

    public ChainRPCImpl (WebSocketClient wsClient) {
        this.wsClient = wsClient;
    }

    public void chainUnstableFollow () {
        Subscription<FollowEvent> subscription = this.wsClient.subscribeToEvent(SubscriptionCalls.unstableFollow(true));
        subscription.handler((Subscription.Event<FollowEvent> event) -> {
            SubscriptionHandlers.unstableFollowHandler(event);
        });
    }

    public String chainUnstableUnfollow () {
        return null;
    }

    public String chainUnstableUnpin () {
        return null;
    }

    public String chainUnstableStorage () {
        return null;
    }

    public String chainUnstableCall () {
        return null;
    }

    public String chainUnstableStopCall () {
        return null;
    }
}

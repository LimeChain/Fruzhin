package org.limechain.rpc.chain.events;

import io.emeraldpay.polkaj.api.Subscription;
import org.limechain.rpc.chain.subscriptions.SubscriptionCalls;
import org.limechain.rpc.chain.subscriptions.SubscriptionHandlers;
import org.limechain.rpc.ws.client.WSClient;
import org.springframework.stereotype.Service;

@Service
public class ChainRPCImpl {
    private final String NodeEndpoint = "";

    private final WSClient wsClient;

    public ChainRPCImpl (WSClient wsClient) {
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

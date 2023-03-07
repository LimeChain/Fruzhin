package com.limechain.rpc.methods.chain;

import com.limechain.rpc.methods.chain.events.FollowEvent;
import com.limechain.rpc.methods.chain.subscriptions.SubscriptionCalls;
import com.limechain.rpc.methods.chain.subscriptions.SubscriptionHandlers;
import com.limechain.rpc.ws.client.WebSocketClient;
import io.emeraldpay.polkaj.api.Subscription;
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

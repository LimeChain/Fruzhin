package org.limechain.rpc.chain.subscriptions;

import io.emeraldpay.polkaj.api.SubscribeCall;
import org.limechain.rpc.chain.events.FollowEvent;

public class SubscriptionCalls {
    public static SubscribeCall<FollowEvent> unstableFollow (Boolean runtimeUpdates) {
        return SubscribeCall.create(FollowEvent.class, "chainHead_unstable_follow", "chainHead_unstable_unfollow", runtimeUpdates);
    }

}

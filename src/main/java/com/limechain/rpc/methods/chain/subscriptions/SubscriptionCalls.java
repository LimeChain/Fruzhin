package com.limechain.rpc.methods.chain.subscriptions;

import com.limechain.rpc.methods.chain.events.FollowEvent;
import io.emeraldpay.polkaj.api.SubscribeCall;

public class SubscriptionCalls {
    public static SubscribeCall<FollowEvent> unstableFollow(Boolean runtimeUpdates) {
        return SubscribeCall.create(FollowEvent.class, "chainHead_unstable_follow", "chainHead_unstable_unfollow", runtimeUpdates);
    }

}

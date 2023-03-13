package com.limechain.rpc.subscriptions;

import com.limechain.rpc.subscriptions.chainhead.events.FollowEvent;
import io.emeraldpay.polkaj.api.SubscribeCall;

public class SubscriptionCalls {
    public static SubscribeCall<FollowEvent> unstableFollow(Boolean runtimeUpdates) {
        return SubscribeCall.create(
                FollowEvent.class,
                "chainHead_unstable_follow",
                "chainHead_unstable_unfollow",
                runtimeUpdates
        );
    }

}

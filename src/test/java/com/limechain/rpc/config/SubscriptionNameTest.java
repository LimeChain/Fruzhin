package com.limechain.rpc.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SubscriptionNameTest {
    @Test
    public void SubscriptionNames_haveCorrectValues() {
        assertEquals(SubscriptionName.CHAIN_HEAD_UNSTABLE_FOLLOW.getValue(), "chainHead_unstable_follow");
        assertEquals(SubscriptionName.CHAIN_HEAD_UNSTABLE_UNFOLLOW.getValue(), "chainHead_unstable_unfollow");
        assertEquals(SubscriptionName.CHAIN_HEAD_UNSTABLE_UNPIN.getValue(), "chainHead_unstable_unpin");
        assertEquals(SubscriptionName.CHAIN_HEAD_UNSTABLE_STORAGE.getValue(), "chainHead_unstable_storage");
        assertEquals(SubscriptionName.CHAIN_HEAD_UNSTABLE_CALL.getValue(), "chainHead_unstable_call");
        assertEquals(SubscriptionName.CHAIN_HEAD_UNSTABLE_STOP_CALL.getValue(), "chainHead_unstable_stopCall");
        assertEquals(SubscriptionName.TRANSACTION_UNSTABLE_SUBMIT_AND_WATCH.getValue(),
                "transaction_unstable_submitAndWatch");
        assertEquals(SubscriptionName.TRANSACTION_UNSTABLE_UNWATCH.getValue(), "transaction_unstable_unwatch");
    }
    
    @Test
    public void FromString_returns_correctValue() {
        assertEquals(SubscriptionName.fromString("chainHead_unstable_follow"),
                SubscriptionName.CHAIN_HEAD_UNSTABLE_FOLLOW);
        assertNull(SubscriptionName.fromString("invalid"));
    }
}
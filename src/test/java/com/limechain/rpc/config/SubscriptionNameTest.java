package com.limechain.rpc.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SubscriptionNameTest {
    @Test
    void SubscriptionNames_haveCorrectValues() {
        assertEquals("chainHead_unstable_follow", SubscriptionName.CHAIN_HEAD_UNSTABLE_FOLLOW.getValue());
        assertEquals("chainHead_unstable_unfollow", SubscriptionName.CHAIN_HEAD_UNSTABLE_UNFOLLOW.getValue());
        assertEquals("chainHead_unstable_unpin", SubscriptionName.CHAIN_HEAD_UNSTABLE_UNPIN.getValue());
        assertEquals("chainHead_unstable_storage", SubscriptionName.CHAIN_HEAD_UNSTABLE_STORAGE.getValue());
        assertEquals("chainHead_unstable_call", SubscriptionName.CHAIN_HEAD_UNSTABLE_CALL.getValue());
        assertEquals("chainHead_unstable_stopCall", SubscriptionName.CHAIN_HEAD_UNSTABLE_STOP_CALL.getValue());
        assertEquals("transaction_unstable_submitAndWatch",
                SubscriptionName.TRANSACTION_UNSTABLE_SUBMIT_AND_WATCH.getValue());
        assertEquals("transaction_unstable_unwatch", SubscriptionName.TRANSACTION_UNSTABLE_UNWATCH.getValue());
    }
    
    @Test
    void FromString_returns_correctValue() {
        assertEquals(SubscriptionName.CHAIN_HEAD_UNSTABLE_FOLLOW,
                SubscriptionName.fromString("chainHead_unstable_follow"));
        assertNull(SubscriptionName.fromString("invalid"));
    }
}
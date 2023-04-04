package com.limechain.rpc.pubsub;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TopicTest {

    @Test
    public void Topics_haveCorrectValues() {
        assertEquals(Topic.UNSTABLE_FOLLOW.getValue(), "unstable_follow");
        assertEquals(Topic.UNSTABLE_TRANSACTION_WATCH.getValue(), "transaction_watch");
    }

    @Test
    public void FromString_returns_correctValue() {
        assertEquals(Topic.fromString("unstable_follow"),
                Topic.UNSTABLE_FOLLOW);
        assertNull(Topic.fromString("invalid"));
    }
}
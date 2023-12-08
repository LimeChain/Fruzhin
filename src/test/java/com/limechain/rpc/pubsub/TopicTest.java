package com.limechain.rpc.pubsub;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TopicTest {

    @Test
    void Topics_haveCorrectValues() {
        assertEquals("unstable_follow", Topic.UNSTABLE_FOLLOW.getValue());
        assertEquals("transaction_watch", Topic.UNSTABLE_TRANSACTION_WATCH.getValue());
    }

    @Test
    void FromString_returns_correctValue() {
        assertEquals(Topic.UNSTABLE_FOLLOW,
                Topic.fromString("unstable_follow"));
        assertNull(Topic.fromString("invalid"));
    }
}
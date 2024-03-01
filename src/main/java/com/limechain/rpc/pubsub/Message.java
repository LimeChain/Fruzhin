package com.limechain.rpc.pubsub;

public record Message(Topic topic, String payload) {
}

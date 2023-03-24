package com.limechain.rpc.pubsub;

import lombok.Getter;

/**
 * Holds all possible subscription topics
 */
@Getter
public enum Topic {
    UNSTABLE_FOLLOW("unstable_follow"),
    UNSTABLE_TRANSACTION_WATCH("transaction_watch");
    private final String value;

    Topic(String value) {
        this.value = value;
    }

    /**
     * Tries to map string parameter to an enum value
     *
     * @param topic name of the enum value to map
     * @return enum value or null if mapping is unsuccessful
     */
    public static Topic fromString(String topic) {
        for (Topic type : values()) {
            if (type.getValue().equals(topic)) {
                return type;
            }
        }
        return null;
    }
}

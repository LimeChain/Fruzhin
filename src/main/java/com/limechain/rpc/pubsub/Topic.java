package com.limechain.rpc.pubsub;

public enum Topic {
    UNSTABLE_FOLLOW("unstable_follow"),
    UNSTABLE_TRANSACTION_WATCH("transaction_watch");
    private final String value;

    Topic(String value) {
        this.value = value;
    }

    public static Topic fromString(String chain) {
        for (Topic type : values()) {
            if (type.getValue().equals(chain)) {
                return type;
            }
        }
        return null;
    }

    public final String getValue() {
        return value;
    }

}

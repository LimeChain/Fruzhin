package com.limechain.rpc.config;

import lombok.Getter;

/**
 * Holds pub-sub rpc method names
 */
@Getter
public enum SubscriptionName {
    CHAIN_HEAD_UNSTABLE_FOLLOW("chainHead_unstable_follow"),
    CHAIN_HEAD_UNSTABLE_UNFOLLOW("chainHead_unstable_unfollow"),
    CHAIN_HEAD_UNSTABLE_UNPIN("chainHead_unstable_unpin"),
    CHAIN_HEAD_UNSTABLE_STORAGE("chainHead_unstable_storage"),
    CHAIN_HEAD_UNSTABLE_CALL("chainHead_unstable_call"),
    CHAIN_HEAD_UNSTABLE_STOP_CALL("chainHead_unstable_stopCall"),
    TRANSACTION_UNSTABLE_SUBMIT_AND_WATCH("transaction_unstable_submitAndWatch"),
    TRANSACTION_UNSTABLE_UNWATCH("transaction_unstable_unwatch");

    /**
     * Holds the name of the rpc method
     */
    private final String value;

    SubscriptionName(String value) {
        this.value = value;
    }

    /**
     * Tries to map string parameter to an enum value
     *
     * @param subscriptionName name of the enum value to map
     * @return {@link SubscriptionName} or null if mapping is unsuccessful
     */
    public static SubscriptionName fromString(String subscriptionName) {
        for (SubscriptionName type : values()) {
            if (type.getValue().equals(subscriptionName)) {
                return type;
            }
        }
        return null;
    }
}

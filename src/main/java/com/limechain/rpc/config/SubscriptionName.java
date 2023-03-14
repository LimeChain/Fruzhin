package com.limechain.rpc.config;

import lombok.Getter;

@Getter
public enum SubscriptionName {
    CHAIN_HEAD_UNSTABLE_FOLLOW("chainHead_unstable_follow"),
    CHAIN_HEAD_UNSTABLE_UNFOLLOW("chainHead_unstable_unfollow"),
    CHAIN_HEAD_UNSTABLE_UNPIN("chainHead_unstable_unpin"),
    CHAIN_HEAD_UNSTABLE_STORAGE("chainHead_unstable_storage"),
    CHAIN_HEAD_UNSTABLE_CALL("chainHead_unstable_call"),
    CHAIN_HEAD_UNSTABLE_STOP_CALL("chainHead_unstable_stopCall"),
    CHAIN_HEAD_UNSTABLE_SUBMIT_AND_WATCH("transaction_unstable_submitAndWatch"),
    CHAIN_HEAD_UNSTABLE_UNWATCH("transaction_unstable_unwatch");

    private final String value;

    SubscriptionName(String value) {
        this.value = value;
    }

    public static SubscriptionName fromString(String chain) {
        for (SubscriptionName type : values()) {
            if (type.getValue().equals(chain)) {
                return type;
            }
        }
        return null;
    }


}

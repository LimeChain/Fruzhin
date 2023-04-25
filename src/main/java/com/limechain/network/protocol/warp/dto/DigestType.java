package com.limechain.network.protocol.warp.dto;

public enum DigestType {
    CONSENSUS_MESSAGE(4), SEAL(5), PRE_RUNTIME(6), RUN_ENV_UPDATED(8), OTHER(0);
    private final int value;

    DigestType(int value) {
        this.value = value;
    }

    public static DigestType fromId(int id) {
        for (DigestType type : values()) {
            if (type.getValue() == id) {
                return type;
            }
        }
        return null;
    }

    public int getValue() {
        return value;
    }

}

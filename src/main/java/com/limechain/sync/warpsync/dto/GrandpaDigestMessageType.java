package com.limechain.sync.warpsync.dto;

import lombok.Getter;

public enum GrandpaDigestMessageType {
    SCHEDULED_CHANGE(1), FORCED_CHANGE(2), ON_DISABLED(3), PAUSE(4), RESUME(5);

    @Getter
    private final int value;

    GrandpaDigestMessageType(int id) {
        value = id;
    }

    public static GrandpaDigestMessageType fromId(int id) {
        for (GrandpaDigestMessageType type : values()) {
            if (type.getValue() == id) {
                return type;
            }
        }
        return null;
    }
}

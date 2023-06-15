package com.limechain.sync.warpsync.dto;

import lombok.Getter;

public enum GrandpaMessageType {
    SCHEDULED_CHANGE(1), FORCED_CHANGE(2), ON_DISABLED(3), PAUSE(4), RESUME(5);

    @Getter
    private final int value;

    GrandpaMessageType(int id) {
        value = id;
    }

    public static GrandpaMessageType fromId(int id) {
        for (GrandpaMessageType type : values()) {
            if (type.getValue() == id) {
                return type;
            }
        }
        return null;
    }
}

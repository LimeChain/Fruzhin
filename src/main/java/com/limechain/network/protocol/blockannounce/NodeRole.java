package com.limechain.network.protocol.blockannounce;

import lombok.Getter;

public enum NodeRole {
    NONE(-1),
    FULL(1),
    LIGHT(2),
    AUTHORING(4);

    @Getter
    private final Integer value;

    NodeRole(Integer value) {
        this.value = value;
    }

}

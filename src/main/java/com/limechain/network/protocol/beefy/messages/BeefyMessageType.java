package com.limechain.network.protocol.beefy.messages;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum BeefyMessageType {

    HANDSHAKE(-1),
    VOTE(0),
    JUSTIFICATION(1);

    private final int type;

    BeefyMessageType(int type) {
        this.type = type;
    }

    public static BeefyMessageType getByType(int type) {
        return Arrays.stream(values())
                .filter(t -> t.type == type)
                .findFirst()
                .orElse(null);
    }
}

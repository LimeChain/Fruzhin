package com.limechain.network.protocol.grandpa.scale;

import lombok.Getter;

import java.util.Arrays;

public enum GrandpaMessageType {
    HANDSHAKE(-1), VOTE(0), COMMIT(1), NEIGHBOUR(2), CATCH_UP_REQUEST(3), CATCH_UP_RESPONSE(4);

    @Getter
    private final int type;

    GrandpaMessageType(int type) {
        this.type = type;
    }

    public static GrandpaMessageType getByType(int type) {
        return Arrays.stream(values()).filter(t -> t.type == type).findFirst().orElse(null);
    }
}

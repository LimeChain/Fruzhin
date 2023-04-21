package com.limechain.network.protocol.warp.dto;

import java.util.Arrays;

public enum ConsensusEngineId {
    BABE(new byte[]{'B', 'A', 'B', 'E'}), GRANDPA(new byte[]{'F', 'R', 'N', 'K'}),
    BEEFY(new byte[]{'B', 'E', 'E', 'F'});

    public static final int ID_LENGTH = 4;
    private final byte[] value;

    ConsensusEngineId(byte[] value) {
        this.value = value;
    }

    public static ConsensusEngineId fromId(byte[] id) {
        for (ConsensusEngineId type : values()) {
            if (Arrays.equals(type.getValue(), id)) {
                return type;
            }
        }
        return null;
    }

    public byte[] getValue() {
        return value;
    }

}

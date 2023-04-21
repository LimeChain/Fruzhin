package com.limechain.network.protocol.warp.dto;

import java.util.Arrays;

public enum ConsensusEngine {
    BABE(new byte[]{'B', 'A', 'B', 'E'}), GRANDPA(new byte[]{'F', 'R', 'N', 'K'}),
    BEEFY(new byte[]{'B', 'E', 'E', 'F'});

    public static final int ID_LENGTH = 4;
    private final byte[] value;

    ConsensusEngine(byte[] value) {
        this.value = value;
    }

    public static ConsensusEngine fromId(byte[] id) {
        for (ConsensusEngine type : values()) {
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

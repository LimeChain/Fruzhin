package com.limechain.network.protocol.beefy.messages.justification;

import lombok.Getter;

import java.util.Arrays;

@Getter
public enum BeefyJustificationVersion {

    V1(1);

    private final int version;

    BeefyJustificationVersion(int version) {
        this.version = version;
    }

    public static BeefyJustificationVersion getByType(int type) {
        return Arrays.stream(values())
                .filter(t -> t.version == type)
                .findFirst()
                .orElse(null);
    }
}

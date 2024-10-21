package com.limechain.babe.consesus;

import lombok.Getter;

@Getter
public enum BabeConsensusMessageFormat {
    ONE((byte) 1), TWO((byte) 2), TREE((byte) 3);

    private final byte format;

    BabeConsensusMessageFormat(byte format) {
        this.format = format;
    }

    public static BabeConsensusMessageFormat fromFormat(byte format) {
        for (BabeConsensusMessageFormat messageFormat : values()) {
            if (messageFormat.getFormat() == format) {
                return messageFormat;
            }
        }
        throw new IllegalArgumentException("Unknown babe consensus message format: " + format);
    }
}

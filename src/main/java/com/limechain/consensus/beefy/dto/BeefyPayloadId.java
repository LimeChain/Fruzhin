package com.limechain.consensus.beefy.dto;

public enum BeefyPayloadId {
    MMR(new byte[]{'m', 'h'});

    private final byte[] id;

    BeefyPayloadId(byte[] id) {
        this.id = id;
    }

    public byte[] getId() {
        return id;
    }

    public static BeefyPayloadId fromBytes(byte[] bytes) {
        for (BeefyPayloadId value : values()) {
            if (java.util.Arrays.equals(value.id, bytes)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown BeefyPayloadId: " + java.util.Arrays.toString(bytes));
    }
}
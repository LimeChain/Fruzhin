package com.limechain.consensus.beefy.dto.message;

import lombok.Getter;

@Getter
public enum BeefyConsensusMessageFormat {
    BEEFY_CHANGED_AUTHORITIES(1), BEEFY_ON_DISABLED(2), BEEFY_MMR_ROOT(3);

    private final int format;

    BeefyConsensusMessageFormat(int format) {
        this.format = format;
    }

    public static BeefyConsensusMessageFormat fromFormat(byte format) {
        for (BeefyConsensusMessageFormat messageFormat : values()) {
            if (messageFormat.getFormat() == format) {
                return messageFormat;
            }
        }
        throw new IllegalArgumentException("Unknown beefy consensus message format: " + format);
    }
}

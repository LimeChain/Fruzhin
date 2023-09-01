package com.limechain.network.protocol.warp.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * Each fragment represents a change in the list of Grandpa authorities, and a list of signatures of
 * the previous authorities that certify that this change is correct
 */
@Getter
@Setter
public class WarpSyncFragment {
    private BlockHeader header;
    private Justification justification;

    @Override
    public String toString() {
        return "WarpSyncFragment{" +
                "header=" + header +
                ", justification=" + justification +
                '}';
    }
}

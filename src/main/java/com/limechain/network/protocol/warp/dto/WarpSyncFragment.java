package com.limechain.network.protocol.warp.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
/**
 * Each fragment represents a change in the list of Grandpa authorities, and a list of signatures of
 * the previous authorities that certify that this change is correct
 */
public class WarpSyncFragment {
    private BlockHeader header;
    private WarpSyncJustification justification;

    @Override
    public String toString() {
        return "WarpSyncFragment{" +
                "header=" + header +
                ", justification=" + justification +
                '}';
    }
}

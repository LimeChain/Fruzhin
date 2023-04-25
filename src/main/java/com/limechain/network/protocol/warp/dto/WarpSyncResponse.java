package com.limechain.network.protocol.warp.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Arrays;

@Getter
@Setter
public class WarpSyncResponse {
    private WarpSyncFragment[] fragments;
    private boolean isFinished;

    @Override
    public String toString() {
        return "WarpSyncResponse{" +
                "fragments=" + Arrays.toString(fragments) +
                ", isFinished=" + isFinished +
                '}';
    }
}

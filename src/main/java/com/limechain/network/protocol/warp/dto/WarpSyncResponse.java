package com.limechain.network.protocol.warp.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WarpSyncResponse {
    private WarpSyncFragment[] fragments;
    private boolean isFinished;
}

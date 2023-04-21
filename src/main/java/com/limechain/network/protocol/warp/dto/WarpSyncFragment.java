package com.limechain.network.protocol.warp.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class WarpSyncFragment {
    private BlockHeader header;
    private WarpSyncJustification justification;
}

package com.limechain.network.protocol.warp.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class WarpSyncFragment {
    private String scaleEncodedHeader;
    private String scaleEncodedJustification;
}

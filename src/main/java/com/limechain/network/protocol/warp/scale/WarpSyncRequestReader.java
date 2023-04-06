package com.limechain.network.protocol.warp.scale;

import com.limechain.network.protocol.warp.dto.WarpSyncResponse;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleReader;

public class WarpSyncRequestReader implements ScaleReader<WarpSyncResponse> {

    @Override
    public WarpSyncResponse read(ScaleCodecReader scaleCodecReader) {
        WarpSyncResponse response = new WarpSyncResponse();
        return response;
    }
}

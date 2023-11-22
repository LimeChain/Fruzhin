package com.limechain.network.protocol.warp.scale.writer;

import com.limechain.network.protocol.warp.dto.WarpSyncRequest;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import io.emeraldpay.polkaj.scale.ScaleWriter;

import java.io.IOException;

public class WarpSyncRequestWriter implements ScaleWriter<WarpSyncRequest> {

    @Override
    public void write(ScaleCodecWriter writer, WarpSyncRequest warpSyncRequest) throws IOException {
        var hashBytes = warpSyncRequest.getBlockHash();

        writer.writeUint256(hashBytes.getBytes());
    }
}

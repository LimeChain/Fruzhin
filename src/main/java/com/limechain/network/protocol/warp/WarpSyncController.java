package com.limechain.network.protocol.warp;

import com.limechain.network.protocol.warp.dto.WarpSyncRequest;
import com.limechain.network.protocol.warp.dto.WarpSyncResponse;

import java.util.concurrent.CompletableFuture;

public interface WarpSyncController {
    CompletableFuture<WarpSyncResponse> send(WarpSyncRequest req);

    default CompletableFuture<WarpSyncResponse> warpSyncRequest(String blockHash) {
        var request = new WarpSyncRequest(blockHash);

        return send(request);

    }
}

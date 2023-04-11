package com.limechain.network.protocol.sync;

import com.google.protobuf.ByteString;
import com.limechain.network.substream.sync.pb.SyncMessage;

import java.util.concurrent.CompletableFuture;

import static java.util.Objects.isNull;

public interface SyncController {
    CompletableFuture<SyncMessage.BlockResponse> send(SyncMessage.BlockRequest msg);

    default CompletableFuture<SyncMessage.BlockResponse> sendBlockRequest(int fields,
                                                                          String fromHash,
                                                                          byte[] fromNumber,
                                                                          byte[] toBlockNumber,
                                                                          SyncMessage.Direction direction,
                                                                          int maxBlocks) {
        var syncMessage = SyncMessage.BlockRequest.newBuilder()
                .setFields(fields)
                .setDirection(direction)
                .setMaxBlocks(maxBlocks);
        if (!isNull(fromHash))
            syncMessage = syncMessage.setHash(ByteString.fromHex(fromHash));
        if (!isNull(fromNumber))
            syncMessage = syncMessage.setNumber(ByteString.copyFrom(fromNumber));
        if (!isNull(toBlockNumber))
            syncMessage = syncMessage.setToBlock(ByteString.copyFrom(toBlockNumber));

        var builtSyncMessage = syncMessage.build();
        return send(builtSyncMessage);
    }
}

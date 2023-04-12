package com.limechain.network.protocol.sync;

import com.google.protobuf.ByteString;
import com.limechain.network.substream.sync.pb.SyncMessage;

import java.util.concurrent.CompletableFuture;

import static java.util.Objects.isNull;

public interface SyncController {
    CompletableFuture<SyncMessage.BlockResponse> send(SyncMessage.BlockRequest msg);

    /**
     * Converts block number to bytes to encode them for the sync message
     * @param blockNumber
     * @return byte array made from the block number
     */
    private static byte[] blockNumberToByteArray(int blockNumber) {
        byte byte1 = (byte) (blockNumber);
        byte byte2 = (byte) (blockNumber >>> 8);
        byte byte3 = (byte) (blockNumber >>> 16);
        byte byte4 = (byte) (blockNumber >>> 24);
        byte byte5 = (byte) (blockNumber >>> 32);
        return new byte[]{byte1, byte2, byte3, byte4, byte5};
    }

    default CompletableFuture<SyncMessage.BlockResponse> sendBlockRequest(int fields,
                                                                          String fromHash,
                                                                          Integer fromNumber,
                                                                          Integer toBlockNumber,
                                                                          SyncMessage.Direction direction,
                                                                          int maxBlocks) {
        var syncMessage = SyncMessage.BlockRequest.newBuilder()
                .setFields(fields)
                .setDirection(direction)
                .setMaxBlocks(maxBlocks);
        if (!isNull(fromHash))
            syncMessage = syncMessage.setHash(ByteString.fromHex(fromHash));
        if (!isNull(fromNumber))
            syncMessage = syncMessage.setNumber(ByteString.copyFrom(blockNumberToByteArray(fromNumber)));
        if (!isNull(toBlockNumber))
            syncMessage = syncMessage.setToBlock(ByteString.copyFrom(blockNumberToByteArray(toBlockNumber)));

        var builtSyncMessage = syncMessage.build();
        return send(builtSyncMessage);
    }
}

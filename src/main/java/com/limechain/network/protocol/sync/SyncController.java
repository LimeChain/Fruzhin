package com.limechain.network.protocol.sync;

import com.google.protobuf.ByteString;
import com.limechain.exception.NotImplementedException;
import com.limechain.network.protocol.sync.pb.SyncMessage;
import com.limechain.utils.LittleEndianUtils;

import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;

import static java.util.Objects.isNull;

public interface SyncController {
    default <T, E> CompletableFuture<E> send(T req, Class<E> resClass){
        throw new NotImplementedException("Method not implemented!");
    }

    default CompletableFuture<SyncMessage.BlockResponse> sendBlockRequest(Integer fields,
                                                                          String fromHash,
                                                                          Integer fromNumber,
                                                                          SyncMessage.Direction direction,
                                                                          int maxBlocks) {
        fields = ByteBuffer.wrap(LittleEndianUtils.intTo32LEBytes(fields)).getInt();
        SyncMessage.BlockRequest.Builder syncMessage = SyncMessage.BlockRequest.newBuilder()
                .setFields(fields)
                .setDirection(direction)
                .setMaxBlocks(maxBlocks);
        if (!isNull(fromHash))
            syncMessage = syncMessage.setHash(ByteString.fromHex(fromHash));
        if (!isNull(fromNumber))
            syncMessage = syncMessage.setNumber(ByteString.copyFrom(LittleEndianUtils.intTo32LEBytes(fromNumber)));

        var builtSyncMessage = syncMessage.build();
        return send(builtSyncMessage, SyncMessage.BlockResponse.class);
    }

    default CompletableFuture<SyncMessage.StateResponse> sendStateRequest(String fromHash) {
        SyncMessage.StateRequest build = SyncMessage.StateRequest
                .newBuilder()
                .setBlock(ByteString.copyFrom(fromHash.getBytes()))
                .build();

        return send(build, SyncMessage.StateResponse.class);
    }

}

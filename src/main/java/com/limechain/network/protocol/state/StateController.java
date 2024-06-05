package com.limechain.network.protocol.state;

import com.google.protobuf.ByteString;
import com.limechain.exception.NotImplementedException;
import com.limechain.network.protocol.sync.pb.SyncMessage;

import java.util.concurrent.CompletableFuture;

public interface StateController {

    default CompletableFuture<SyncMessage.StateResponse> send(SyncMessage.StateRequest req){
        throw new NotImplementedException("Method not implemented!");
    }

    default CompletableFuture<SyncMessage.StateResponse> sendStateRequest(String fromHash) {
        SyncMessage.StateRequest build = SyncMessage.StateRequest
                .newBuilder()
                .setBlock(ByteString.fromHex(fromHash))
                .setNoProof(true)
                .build();

        return send(build);
    }
}

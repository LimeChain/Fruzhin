package com.limechain.network.substream.lightclient;

import com.google.protobuf.ByteString;
import com.limechain.network.substream.lightclient.pb.LightClientMessage;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public interface LightMessagesController {
    CompletableFuture<LightClientMessage.Response> send(LightClientMessage.Request msg);

    default CompletableFuture<LightClientMessage.Response> remoteCallRequest(String blockHash,
                                                                             String methodName,
                                                                             String callData) {
        return send(LightClientMessage.Request
                .newBuilder()
                .setRemoteCallRequest(
                        LightClientMessage.RemoteCallRequest
                                .newBuilder()
                                .setBlock(ByteString.copyFrom(blockHash.getBytes()))
                                .setMethod(methodName)
                                .setData(ByteString.copyFrom(callData.getBytes()))
                                .build()
                )
                .build());
    }

    default CompletableFuture<LightClientMessage.Response> remoteReadRequest(String blockHash,
                                                                             String[] storageKeys) {

        return send(LightClientMessage.Request
                .newBuilder()
                .setRemoteReadRequest(
                        LightClientMessage.RemoteReadRequest
                                .newBuilder()
                                .setBlock(ByteString.copyFrom(blockHash.getBytes()))
                                .addAllKeys(
                                        Arrays.stream(storageKeys)
                                                .map(s -> ByteString.copyFrom(s.getBytes()))
                                                .collect(Collectors.toList())
                                )
                                .build()
                )
                .build());

    }

    default CompletableFuture<LightClientMessage.Response> remoteReadChildRequest(String blockHash,
                                                                                  String childStorageKey,
                                                                                  String[] keys) {
        return send(LightClientMessage.Request
                .newBuilder()
                .setRemoteReadChildRequest(
                        LightClientMessage.RemoteReadChildRequest
                                .newBuilder()
                                .setBlock(ByteString.copyFrom(blockHash.getBytes()))
                                .setStorageKey(ByteString.copyFrom(childStorageKey.getBytes()))
                                .addAllKeys(
                                        Arrays.stream(keys)
                                                .map(s -> ByteString.copyFrom(s.getBytes()))
                                                .collect(Collectors.toList())
                                )
                                .build()
                )
                .build());
    }

}

package com.limechain.network.substream.lightclient;

import com.google.protobuf.ByteString;
import com.limechain.network.substream.lightclient.pb.LightClientMessage;
import io.libp2p.core.Stream;
import lombok.extern.java.Log;

import java.util.logging.Level;

@Log
public class LightMessagesEngine {

    // TODO: Add logic for handling incoming each request type
    public void receiveRequest(LightClientMessage.Request msg, Stream stream) {
        var builder = LightClientMessage.Response.newBuilder();
        if (msg.hasRemoteCallRequest()) {
            log.log(Level.INFO, "Received: RemoteCallRequest");
            builder.setRemoteCallResponse(
                    LightClientMessage.RemoteCallResponse.newBuilder()
                            .setProof(ByteString.copyFrom("0x0".getBytes()))
                            .build());
        } else if (msg.hasRemoteReadRequest()) {
            log.log(Level.INFO, "Received: RemoteReadRequest");
            builder.setRemoteReadResponse(
                    LightClientMessage.RemoteReadResponse.newBuilder()
                            .setProof(ByteString.copyFrom("0x1".getBytes()))
                            .build());
        } else if (msg.hasRemoteReadChildRequest()) {
            log.log(Level.INFO, "Received: RemoteReadChildRequest");
            builder.setRemoteReadResponse(
                    LightClientMessage.RemoteReadResponse.newBuilder()
                            .setProof(ByteString.copyFrom("0x2".getBytes()))
                            .build());
        }
        stream.writeAndFlush(builder.build());
    }
}

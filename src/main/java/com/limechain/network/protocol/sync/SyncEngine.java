package com.limechain.network.protocol.sync;

import com.limechain.network.substream.sync.pb.SyncMessage;
import io.libp2p.core.Stream;
import lombok.extern.java.Log;

import java.util.logging.Level;

@Log
public class SyncEngine {
    public void receiveRequest(SyncMessage.BlockRequest msg, Stream stream){
        var builder = SyncMessage.BlockResponse.newBuilder();
        log.log(Level.INFO, "Receive: BlockResponse");
        System.out.println(msg.toString());
        stream.writeAndFlush(builder.build());
    }
}

package com.limechain.network.protocol.blockannounce;

import com.limechain.network.StrictProtocolBinding;
import lombok.extern.java.Log;

@Log
public class BlockAnnounce extends StrictProtocolBinding<BlockAnnounceController> {
    public BlockAnnounce(String protocolId, BlockAnnounceProtocol protocol) {
        super(protocolId, protocol);
    }
}

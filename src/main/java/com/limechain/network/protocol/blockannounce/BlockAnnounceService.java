package com.limechain.network.protocol.blockannounce;

import com.limechain.network.protocol.NetworkService;

public class BlockAnnounceService extends NetworkService<BlockAnnounce> {
    public BlockAnnounceService(String protocolId) {
        this.protocol = new BlockAnnounce(protocolId, new BlockAnnounceProtocol());
    }
}

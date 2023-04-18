package com.limechain.network.protocol.blockannounce;

import io.libp2p.core.multistream.ProtocolBinding;

public class BlockAnnounceService {
    private final BlockAnnounce blockAnnounce;

    public BlockAnnounceService(String protocolId) {
        this.blockAnnounce = new BlockAnnounce(protocolId, new BlockAnnounceProtocol());
    }

    public ProtocolBinding getProtocol() {
        return this.blockAnnounce;
    }
}

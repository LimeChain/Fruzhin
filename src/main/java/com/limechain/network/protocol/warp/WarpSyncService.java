package com.limechain.network.protocol.warp;

import com.limechain.network.protocol.NetworkService;
import io.libp2p.core.multistream.ProtocolBinding;
import lombok.Getter;

public class WarpSyncService implements NetworkService {

    @Getter
    private final WarpSync warpSync;

    public WarpSyncService(String protocolId) {
        this.warpSync = new WarpSync(protocolId, new WarpSyncProtocol());
    }

    public ProtocolBinding getProtocol() {
        return this.warpSync;
    }
}

package com.limechain.network.protocol.warp;

import com.limechain.network.protocol.NetworkService;

public class WarpSyncService implements NetworkService {
    private final WarpSync warpSync;

    public WarpSyncService(String protocolId) {
        this.warpSync = new WarpSync(protocolId, new WarpSyncProtocol());
    }

    public WarpSync getProtocol() {
        return this.warpSync;
    }
}

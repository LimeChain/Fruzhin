package com.limechain.network.protocol.warp;

public class WarpSyncService {
    private final WarpSync warpSync;

    public WarpSyncService() {
        this.warpSync = new WarpSync(new WarpSyncProtocol());
    }
}

package com.limechain.network.protocol.warp;

import com.limechain.network.protocol.NetworkService;

public class WarpSyncService extends NetworkService<WarpSync> {

    public WarpSyncService(String protocolId) {
        this.protocol = new WarpSync(protocolId, new WarpSyncProtocol());
    }

}

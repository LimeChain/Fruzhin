package com.limechain.network.protocol.sync;

import com.limechain.network.protocol.NetworkService;

public class SyncService extends NetworkService<SyncMessages> {
    public SyncService(String protocolId) {
        this.protocol = new SyncMessages(protocolId, new SyncProtocol());
    }
}

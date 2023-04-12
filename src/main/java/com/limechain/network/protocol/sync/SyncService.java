package com.limechain.network.protocol.sync;

import lombok.Getter;

@Getter
public class SyncService {
    private final SyncMessages syncMessages;

    public SyncService(String protocolId) {
        this.syncMessages = new SyncMessages(protocolId, new SyncProtocol());
    }
}

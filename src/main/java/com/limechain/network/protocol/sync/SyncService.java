package com.limechain.network.protocol.sync;

import com.limechain.network.protocol.NetworkService;
import io.libp2p.core.multistream.ProtocolBinding;

public class SyncService implements NetworkService {
    private final SyncMessages syncMessages;

    public SyncService(String protocolId) {
        this.syncMessages = new SyncMessages(protocolId, new SyncProtocol());
    }

    @Override
    public ProtocolBinding getProtocol() {
        return syncMessages;
    }
}

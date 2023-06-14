package com.limechain.network.protocol.lightclient;

import com.limechain.network.protocol.NetworkService;
import io.libp2p.core.multistream.ProtocolBinding;
import lombok.Getter;

public class LightMessagesService implements NetworkService {

    @Getter
    private final LightMessages lightMessages;

    public LightMessagesService(String protocolId) {
        this.lightMessages = new LightMessages(protocolId, new LightMessagesProtocol());
    }

    public ProtocolBinding getProtocol() {
        return lightMessages;
    }
}

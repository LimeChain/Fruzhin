package com.limechain.network.protocol.lightclient;

import com.limechain.network.protocol.NetworkService;

public class LightMessagesService extends NetworkService<LightMessages> {
    public LightMessagesService(String protocolId) {
        this.protocol = new LightMessages(protocolId, new LightMessagesProtocol());
    }
}

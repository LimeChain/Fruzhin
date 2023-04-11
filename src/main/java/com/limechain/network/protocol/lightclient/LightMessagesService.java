package com.limechain.network.protocol.lightclient;

import lombok.Getter;

public class LightMessagesService {
    @Getter
    private final LightMessages lightMessages;

    public LightMessagesService() {
        this.lightMessages = new LightMessages(new LightMessagesProtocol());
    }
}

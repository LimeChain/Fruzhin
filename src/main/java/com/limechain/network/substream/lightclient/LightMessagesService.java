package com.limechain.network.substream.lightclient;

import lombok.Getter;

public class LightMessagesService {
    @Getter
    private final LightMessages lightMessages;

    public LightMessagesService() {
        this.lightMessages = new LightMessages(new LightMessagesProtocol(new LightMessagesEngine()));
    }
}

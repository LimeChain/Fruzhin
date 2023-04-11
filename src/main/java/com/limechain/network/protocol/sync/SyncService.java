package com.limechain.network.protocol.sync;

import lombok.Getter;

@Getter
public class SyncService {
    private final SyncMessages syncMessages;

    public SyncService(){
        this.syncMessages = new SyncMessages(new SyncProtocol(new SyncEngine()));
    }
}

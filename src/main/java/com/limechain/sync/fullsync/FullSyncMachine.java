package com.limechain.sync.fullsync;

import com.limechain.network.Network;
import com.limechain.network.protocol.warp.dto.Block;
import lombok.Getter;

//TODO: Implement FullSyncMachine
@Getter
public class FullSyncMachine {
    private final Network networkService;

    public FullSyncMachine(final Network networkService) {
        this.networkService = networkService;
    }

    public void start() {
        networkService.sendNeighbourMessages();
    }
}

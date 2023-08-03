package com.limechain.network.protocol.grandpa;

import com.limechain.network.protocol.blockannounce.scale.BlockAnnounceHandshake;

public interface GrandpaController {
    void sendHandshake(BlockAnnounceHandshake req);

    void sendNeighbourMessage();

}

package com.limechain.network.protocol.blockannounce;

import com.limechain.network.protocol.blockannounce.scale.BlockAnnounceHandshake;

public interface BlockAnnounceController {
    void sendHandshake(BlockAnnounceHandshake req);
}

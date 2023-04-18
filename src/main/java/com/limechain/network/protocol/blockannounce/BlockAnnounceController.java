package com.limechain.network.protocol.blockannounce;

import com.limechain.network.protocol.blockannounce.scale.BlockAnnounceHandShake;

public interface BlockAnnounceController {
    void sendHandshake(BlockAnnounceHandShake req);
}

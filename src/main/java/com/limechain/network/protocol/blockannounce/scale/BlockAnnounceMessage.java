package com.limechain.network.protocol.blockannounce.scale;

import com.limechain.network.protocol.warp.dto.BlockHeader;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BlockAnnounceMessage {
    private BlockHeader header;
    private boolean bestBlock;

    @Override
    public String toString() {
        return "BlockAnnounceMessage{" +
                "header=" + header +
                ", isBestBlock=" + bestBlock +
                '}';
    }
}

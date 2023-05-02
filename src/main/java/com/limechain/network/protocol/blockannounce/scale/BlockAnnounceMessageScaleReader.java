package com.limechain.network.protocol.blockannounce.scale;

import com.limechain.network.protocol.warp.scale.BlockHeaderReader;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleReader;

public class BlockAnnounceMessageScaleReader implements ScaleReader<BlockAnnounceMessage> {
    @Override
    public BlockAnnounceMessage read(ScaleCodecReader reader) {
        BlockAnnounceMessage message = new BlockAnnounceMessage();
        message.header = new BlockHeaderReader().read(reader);
        message.isBestBlock = reader.readBoolean();
        System.out.println("Decoded: " + message);
        return message;
    }
}

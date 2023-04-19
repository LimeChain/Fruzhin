package com.limechain.network.protocol.blockannounce.scale;

import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleReader;

public class BlockAnnounceMessageScaleReader implements ScaleReader<BlockAnnounceMessage> {
    @Override
    public BlockAnnounceMessage read(ScaleCodecReader reader) {
        BlockAnnounceMessage message = new BlockAnnounceMessage();
        message.header = new BlockHeaderScaleReader().read(reader);
        message.isBestBlock = reader.readBoolean();
        System.out.println("Decoded: " + message);
        return message;
    }
}

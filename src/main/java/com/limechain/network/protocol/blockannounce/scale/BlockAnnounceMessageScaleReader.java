package com.limechain.network.protocol.blockannounce.scale;

import com.limechain.network.protocol.warp.scale.reader.BlockHeaderReader;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleReader;

public class BlockAnnounceMessageScaleReader implements ScaleReader<BlockAnnounceMessage> {
    @Override
    public BlockAnnounceMessage read(ScaleCodecReader reader) {
        BlockAnnounceMessage message = new BlockAnnounceMessage();
        message.setHeader(new BlockHeaderReader().read(reader));
        message.setBestBlock(reader.readBoolean());
        return message;
    }
}

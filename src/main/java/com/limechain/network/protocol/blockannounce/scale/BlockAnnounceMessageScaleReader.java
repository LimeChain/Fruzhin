package com.limechain.network.protocol.blockannounce.scale;

import com.limechain.network.protocol.blockannounce.messages.BlockAnnounceMessage;
import com.limechain.network.protocol.warp.scale.reader.BlockHeaderReader;
import com.limechain.polkaj.reader.ScaleCodecReader;
import com.limechain.polkaj.reader.ScaleReader;

public class BlockAnnounceMessageScaleReader implements ScaleReader<BlockAnnounceMessage> {
    @Override
    public BlockAnnounceMessage read(ScaleCodecReader reader) {
        BlockAnnounceMessage message = new BlockAnnounceMessage();
        message.setHeader(new BlockHeaderReader().read(reader));
        message.setBestBlock(reader.readBoolean());
        return message;
    }
}

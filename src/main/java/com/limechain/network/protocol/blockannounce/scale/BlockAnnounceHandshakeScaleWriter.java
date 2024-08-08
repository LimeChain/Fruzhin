package com.limechain.network.protocol.blockannounce.scale;

import com.limechain.network.protocol.blockannounce.messages.BlockAnnounceHandshake;
import com.limechain.polkaj.writer.ScaleCodecWriter;
import com.limechain.polkaj.writer.ScaleWriter;

import java.io.IOException;

public class BlockAnnounceHandshakeScaleWriter implements ScaleWriter<BlockAnnounceHandshake> {
    @Override
    public void write(ScaleCodecWriter writer, BlockAnnounceHandshake handshake) throws IOException {
        writer.writeByte(handshake.getNodeRole());
        writer.writeUint32(handshake.getBestBlock().longValue());
        writer.writeUint256(handshake.getBestBlockHash().getBytes());
        writer.writeUint256(handshake.getGenesisBlockHash().getBytes());
    }
}

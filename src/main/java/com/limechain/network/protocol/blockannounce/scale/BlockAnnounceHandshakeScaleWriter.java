package com.limechain.network.protocol.blockannounce.scale;

import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import io.emeraldpay.polkaj.scale.ScaleWriter;

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

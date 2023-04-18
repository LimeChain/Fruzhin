package com.limechain.network.protocol.blockannounce.scale;

import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import io.emeraldpay.polkaj.scale.ScaleWriter;

import java.io.IOException;

public class BlockAnnounceHandshakeScaleWriter implements ScaleWriter<BlockAnnounceHandShake> {
    @Override
    public void write(ScaleCodecWriter writer, BlockAnnounceHandShake handshake) throws IOException {
        writer.writeByte(handshake.nodeRole);
        writer.writeUint32(Long.parseLong(handshake.bestBlock));
        writer.writeUint256(handshake.bestBlockHash.getBytes());
        writer.writeUint256(handshake.genesisBlockHash.getBytes());
    }
}

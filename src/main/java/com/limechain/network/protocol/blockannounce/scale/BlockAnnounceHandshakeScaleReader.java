package com.limechain.network.protocol.blockannounce.scale;

import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleReader;
import io.emeraldpay.polkaj.types.Hash256;

public class BlockAnnounceHandshakeScaleReader implements ScaleReader<BlockAnnounceHandshake> {
    @Override
    public BlockAnnounceHandshake read(ScaleCodecReader reader) {
        BlockAnnounceHandshake handshake = new BlockAnnounceHandshake();
        handshake.nodeRole = reader.readByte();
        handshake.bestBlock = Long.toString(reader.readUint32());
        handshake.bestBlockHash = new Hash256(reader.readUint256());
        handshake.genesisBlockHash = new Hash256(reader.readUint256());
        return handshake;
    }
}

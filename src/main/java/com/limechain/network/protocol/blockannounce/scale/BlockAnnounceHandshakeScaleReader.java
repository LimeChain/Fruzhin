package com.limechain.network.protocol.blockannounce.scale;

import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleReader;
import io.emeraldpay.polkaj.types.Hash256;

public class BlockAnnounceHandshakeScaleReader implements ScaleReader<BlockAnnounceHandShake> {
    @Override
    public BlockAnnounceHandShake read(ScaleCodecReader reader) {
        BlockAnnounceHandShake handShake = new BlockAnnounceHandShake();
        handShake.nodeRole = reader.readByte();
        handShake.bestBlock = Long.toString(reader.readUint32());
        handShake.bestBlockHash = new Hash256(reader.readUint256());
        handShake.genesisBlockHash = new Hash256(reader.readUint256());
        System.out.println("Decoded " + handShake);
        return handShake;
    }
}

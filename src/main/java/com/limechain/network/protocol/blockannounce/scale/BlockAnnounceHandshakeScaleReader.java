package com.limechain.network.protocol.blockannounce.scale;

import com.limechain.network.protocol.blockannounce.messages.BlockAnnounceHandshake;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleReader;
import io.emeraldpay.polkaj.types.Hash256;

import java.math.BigInteger;

public class BlockAnnounceHandshakeScaleReader implements ScaleReader<BlockAnnounceHandshake> {
    @Override
    public BlockAnnounceHandshake read(ScaleCodecReader reader) {
        BlockAnnounceHandshake handshake = new BlockAnnounceHandshake();
        handshake.setNodeRole(reader.readByte());
        handshake.setBestBlock(BigInteger.valueOf(reader.readUint32()));
        handshake.setBestBlockHash(new Hash256(reader.readUint256()));
        handshake.setGenesisBlockHash(new Hash256(reader.readUint256()));
        return handshake;
    }
}

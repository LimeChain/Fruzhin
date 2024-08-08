package com.limechain.network.protocol.blockannounce.scale;

import com.limechain.network.protocol.blockannounce.messages.BlockAnnounceHandshake;
import com.limechain.polkaj.Hash256;
import com.limechain.polkaj.reader.ScaleCodecReader;
import com.limechain.polkaj.reader.ScaleReader;

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

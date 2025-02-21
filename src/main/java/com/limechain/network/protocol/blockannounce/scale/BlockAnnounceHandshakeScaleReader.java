package com.limechain.network.protocol.blockannounce.scale;

import com.limechain.network.protocol.blockannounce.messages.Handshake;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleReader;
import io.emeraldpay.polkaj.types.Hash256;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BlockAnnounceHandshakeScaleReader implements ScaleReader<Handshake> {

    private static final BlockAnnounceHandshakeScaleReader INSTANCE = new BlockAnnounceHandshakeScaleReader();

    public static BlockAnnounceHandshakeScaleReader getInstance() {
        return INSTANCE;
    }

    @Override
    public Handshake read(ScaleCodecReader reader) {
        Handshake handshake = new Handshake();
        handshake.setNodeRole(reader.readByte());
        handshake.setBestBlock(BigInteger.valueOf(reader.readUint32()));
        handshake.setBestBlockHash(new Hash256(reader.readUint256()));
        handshake.setGenesisBlockHash(new Hash256(reader.readUint256()));
        return handshake;
    }
}

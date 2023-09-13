package com.limechain.network.protocol.grandpa.messages.vote;

import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleReader;
import io.emeraldpay.polkaj.types.Hash256;
import io.emeraldpay.polkaj.types.Hash512;

import java.math.BigInteger;

public class SignedMessageScaleReader implements ScaleReader<SignedMessage> {
    private static final SignedMessageScaleReader INSTANCE = new SignedMessageScaleReader();

    private SignedMessageScaleReader() {
    }

    public static SignedMessageScaleReader getInstance() {
        return INSTANCE;
    }

    @Override
    public SignedMessage read(ScaleCodecReader reader) {

        SignedMessage signedMessage = new SignedMessage();
        signedMessage.setStage(Subround.getByStage(reader.readByte()));
        signedMessage.setBlockHash(new Hash256(reader.readUint256()));
        signedMessage.setBlockNumber(BigInteger.valueOf(reader.readUint32()));
        signedMessage.setSignature(new Hash512(reader.readByteArray(64)));
        signedMessage.setAuthorityPublicKey(new Hash256(reader.readUint256()));

        return signedMessage;
    }
}

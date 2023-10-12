package com.limechain.network.protocol.grandpa.messages.vote;

import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import io.emeraldpay.polkaj.scale.ScaleWriter;

import java.io.IOException;

public class SignedMessageScaleWriter implements ScaleWriter<SignedMessage> {

    private static final SignedMessageScaleWriter INSTANCE = new SignedMessageScaleWriter();

    private SignedMessageScaleWriter() {
    }

    public static SignedMessageScaleWriter getInstance() {
        return INSTANCE;
    }

    @Override
    public void write(ScaleCodecWriter writer, SignedMessage signedMessage) throws IOException {
        writer.writeByte(signedMessage.getStage().getStage());
        writer.writeUint256(signedMessage.getBlockHash().getBytes());
        writer.writeUint32(signedMessage.getBlockNumber().longValue());
        writer.writeByteArray(signedMessage.getSignature().getBytes());
        writer.writeUint256(signedMessage.getAuthorityPublicKey().getBytes());
    }
}

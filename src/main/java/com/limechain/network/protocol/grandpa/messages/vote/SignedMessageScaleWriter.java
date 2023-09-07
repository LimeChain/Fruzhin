package com.limechain.network.protocol.grandpa.messages.vote;

import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import io.emeraldpay.polkaj.scale.ScaleWriter;
import io.emeraldpay.polkaj.scale.writer.UInt64Writer;

import java.io.IOException;

public class SignedMessageScaleWriter implements ScaleWriter<SignedMessage> {
    @Override
    public void write(ScaleCodecWriter writer, SignedMessage signedMessage) throws IOException {
        writer.writeByte(signedMessage.getStage().getStage());
        writer.writeUint256(signedMessage.getBlockHash().getBytes());
        new UInt64Writer().write(writer, signedMessage.getBlockNumber());
        writer.writeByteArray(signedMessage.getSignature().getBytes());
        writer.writeUint256(signedMessage.getAuthorityPublicKey().getBytes());
    }
}

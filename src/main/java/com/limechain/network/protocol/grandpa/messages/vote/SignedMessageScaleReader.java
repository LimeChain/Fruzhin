package com.limechain.network.protocol.grandpa.messages.vote;

import com.limechain.network.protocol.warp.scale.VarUint64Reader;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleReader;
import io.emeraldpay.polkaj.types.Hash256;
import io.emeraldpay.polkaj.types.Hash512;

public class SignedMessageScaleReader  implements ScaleReader<SignedMessage> {
    @Override
    public SignedMessage read(ScaleCodecReader reader) {

        SignedMessage signedMessage = new SignedMessage();
        signedMessage.setStage(Subround.getByStage(reader.readByte()));
        signedMessage.setBlockHash(new Hash256(reader.readUint256()));
        signedMessage.setBlockNumber(new VarUint64Reader(4).read(reader));
        signedMessage.setSignature(new Hash512(reader.readByteArray(64)));
        signedMessage.setAuthorityPublicKey(new Hash256(reader.readUint256()));

        return signedMessage;
    }
}

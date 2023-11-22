package com.limechain.network.protocol.warp.scale.reader;

import com.limechain.network.protocol.warp.dto.Precommit;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleReader;
import io.emeraldpay.polkaj.types.Hash256;
import io.emeraldpay.polkaj.types.Hash512;

public class PrecommitReader implements ScaleReader<Precommit> {
    @Override
    public Precommit read(ScaleCodecReader reader) {
        Precommit precommit = new Precommit();
        precommit.setTargetHash(new Hash256(reader.readUint256()));
        precommit.setTargetNumber(new VarUint64Reader(4).read(reader));
        precommit.setSignature(new Hash512(reader.readByteArray(64)));
        precommit.setAuthorityPublicKey(new Hash256(reader.readUint256()));
        return precommit;
    }
}

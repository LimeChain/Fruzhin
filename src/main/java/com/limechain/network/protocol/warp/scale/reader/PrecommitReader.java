package com.limechain.network.protocol.warp.scale.reader;

import com.limechain.network.protocol.warp.dto.Precommit;
import com.limechain.polkaj.Hash256;
import com.limechain.polkaj.Hash512;
import com.limechain.polkaj.reader.ScaleCodecReader;
import com.limechain.polkaj.reader.ScaleReader;

public class PrecommitReader implements ScaleReader<Precommit> {
    @Override
    public Precommit read(ScaleCodecReader reader) {
        Precommit precommit = new Precommit();
        precommit.setTargetHash(new Hash256(reader.readUint256()));
        precommit.setTargetNumber(BlockNumberReader.getInstance().read(reader));
        precommit.setSignature(new Hash512(reader.readByteArray(64)));
        precommit.setAuthorityPublicKey(new Hash256(reader.readUint256()));
        return precommit;
    }
}

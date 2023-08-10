package com.limechain.network.protocol.grandpa.messages.commit;

import com.limechain.network.protocol.warp.dto.Precommit;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleReader;
import io.emeraldpay.polkaj.types.Hash256;
import io.emeraldpay.polkaj.types.Hash512;

import java.math.BigInteger;

public class CompactJustificationScaleReader implements ScaleReader<Precommit[]> {

    @Override
    public Precommit[] read(ScaleCodecReader reader) {
        VoteScaleReader voteScaleReader = new VoteScaleReader();
        int precommitsCount = reader.readCompactInt();
        Precommit[] precommits = new Precommit[precommitsCount];

        for (int i = 0; i < precommitsCount; i++) {
            Vote vote = voteScaleReader.read(reader);
            precommits[i] = new Precommit();
            precommits[i].setTargetHash(vote.getBlockHash());
            precommits[i].setTargetNumber(BigInteger.valueOf(vote.getBlockNumber()));
        }

        if (reader.readCompactInt() != precommitsCount) {
            throw new RuntimeException();
        }

        for (int i = 0; i < precommitsCount; i++) {
            precommits[i].setSignature(new Hash512(reader.readByteArray(64)));
            precommits[i].setAuthorityPublicKey(new Hash256(reader.readUint256()));
        }

        return precommits;
    }
}

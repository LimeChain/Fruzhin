package com.limechain.network.protocol.grandpa.messages.commit;

import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleReader;
import io.emeraldpay.polkaj.scale.reader.UInt32Reader;
import io.emeraldpay.polkaj.types.Hash256;

import java.math.BigInteger;

public class VoteScaleReader implements ScaleReader<Vote> {
    @Override
    public Vote read(ScaleCodecReader reader) {
        Vote vote = new Vote();
        vote.setBlockHash(new Hash256(reader.readUint256()));
        vote.setBlockNumber(BigInteger.valueOf(new UInt32Reader().read(reader)));
        return vote;
    }
}

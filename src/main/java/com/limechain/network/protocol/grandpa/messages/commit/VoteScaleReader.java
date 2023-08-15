package com.limechain.network.protocol.grandpa.messages.commit;

import com.limechain.network.protocol.warp.scale.VarUint64Reader;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleReader;
import io.emeraldpay.polkaj.types.Hash256;

public class VoteScaleReader implements ScaleReader<Vote> {
    @Override
    public Vote read(ScaleCodecReader reader) {
        Vote vote = new Vote();
        vote.setBlockHash(new Hash256(reader.readUint256()));
        vote.setBlockNumber(new VarUint64Reader(4).read(reader));
        return vote;
    }
}
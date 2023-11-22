package com.limechain.network.protocol.grandpa.messages.commit;

import com.limechain.network.protocol.warp.scale.reader.VarUint64Reader;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleReader;
import io.emeraldpay.polkaj.types.Hash256;

public class VoteScaleReader implements ScaleReader<Vote> {
    private static final VoteScaleReader INSTANCE = new VoteScaleReader();
    private final VarUint64Reader varUint64Reader;

    private VoteScaleReader() {
        varUint64Reader = new VarUint64Reader(4);
    }

    public static VoteScaleReader getInstance() {
        return INSTANCE;
    }

    @Override
    public Vote read(ScaleCodecReader reader) {
        Vote vote = new Vote();
        vote.setBlockHash(new Hash256(reader.readUint256()));
        vote.setBlockNumber(varUint64Reader.read(reader));
        return vote;
    }
}

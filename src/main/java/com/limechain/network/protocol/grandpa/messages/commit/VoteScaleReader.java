package com.limechain.network.protocol.grandpa.messages.commit;

import com.limechain.network.protocol.warp.scale.reader.BlockNumberReader;
import com.limechain.polkaj.Hash256;
import com.limechain.polkaj.reader.ScaleCodecReader;
import com.limechain.polkaj.reader.ScaleReader;

public class VoteScaleReader implements ScaleReader<Vote> {
    private static final VoteScaleReader INSTANCE = new VoteScaleReader();

    public static VoteScaleReader getInstance() {
        return INSTANCE;
    }

    @Override
    public Vote read(ScaleCodecReader reader) {
        Vote vote = new Vote();
        vote.setBlockHash(new Hash256(reader.readUint256()));
        vote.setBlockNumber(BlockNumberReader.getInstance().read(reader));
        return vote;
    }
}

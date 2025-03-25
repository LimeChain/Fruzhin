package com.limechain.consensus.grandpa.scale;

import com.limechain.consensus.grandpa.dto.Vote;
import com.limechain.network.protocol.warp.scale.reader.BlockNumberReader;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleReader;
import io.emeraldpay.polkaj.types.Hash256;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
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

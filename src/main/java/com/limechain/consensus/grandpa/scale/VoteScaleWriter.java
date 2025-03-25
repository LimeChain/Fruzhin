package com.limechain.consensus.grandpa.scale;

import com.limechain.consensus.grandpa.dto.Vote;
import com.limechain.network.protocol.warp.scale.writer.BlockNumberWriter;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import io.emeraldpay.polkaj.scale.ScaleWriter;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.IOException;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class VoteScaleWriter implements ScaleWriter<Vote> {

    private static final VoteScaleWriter INSTANCE = new VoteScaleWriter();

    public static VoteScaleWriter getInstance() {
        return INSTANCE;
    }

    @Override
    public void write(ScaleCodecWriter writer, Vote vote) throws IOException {
        writer.writeUint256(vote.getBlockHash().getBytes());
        BlockNumberWriter.getInstance().write(writer, vote.getBlockNumber());
    }
}

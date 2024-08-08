package com.limechain.network.protocol.grandpa.messages.commit;

import com.limechain.network.protocol.warp.scale.writer.BlockNumberWriter;
import com.limechain.polkaj.writer.ScaleCodecWriter;
import com.limechain.polkaj.writer.ScaleWriter;

import java.io.IOException;

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

package com.limechain.network.protocol.grandpa.messages.commit;

import com.limechain.network.protocol.warp.scale.VarUint64Writer;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import io.emeraldpay.polkaj.scale.ScaleWriter;

import java.io.IOException;

public class VoteScaleWriter implements ScaleWriter<Vote> {

    private static final VoteScaleWriter INSTANCE = new VoteScaleWriter();

    private final VarUint64Writer varUint64Writer;

    private VoteScaleWriter() {
        varUint64Writer = new VarUint64Writer(4);
    }

    public static VoteScaleWriter getInstance() {
        return INSTANCE;
    }

    @Override
    public void write(ScaleCodecWriter writer, Vote vote) throws IOException {
        writer.writeUint256(vote.getBlockHash().getBytes());
        varUint64Writer.write(writer, vote.getBlockNumber());
    }
}

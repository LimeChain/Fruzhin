package com.limechain.network.protocol.grandpa.messages.commit;

import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import io.emeraldpay.polkaj.scale.ScaleWriter;

import java.io.IOException;

public class VoteScaleWriter implements ScaleWriter<Vote> {

    @Override
    public void write(ScaleCodecWriter writer, Vote vote) throws IOException {
        writer.writeUint256(vote.getBlockHash().getBytes());
        writer.writeUint32(vote.getBlockNumber().longValue());
    }
}

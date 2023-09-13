package com.limechain.network.protocol.grandpa.messages.catchup.res;

import com.limechain.network.protocol.grandpa.messages.GrandpaMessageType;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import io.emeraldpay.polkaj.scale.ScaleWriter;
import io.emeraldpay.polkaj.scale.writer.ListWriter;
import io.emeraldpay.polkaj.scale.writer.UInt64Writer;

import java.io.IOException;
import java.util.Arrays;

public class CatchUpMessageScaleWriter implements ScaleWriter<CatchUpMessage> {
    @Override
    public void write(ScaleCodecWriter writer, CatchUpMessage catchUpMessage) throws IOException {
        writer.writeByte(GrandpaMessageType.CATCH_UP_RESPONSE.getType());
        new UInt64Writer().write(writer, catchUpMessage.getSetId());
        new UInt64Writer().write(writer, catchUpMessage.getRound());
        new ListWriter<>(new SignedVoteScaleWriter()).write(writer, Arrays.asList(catchUpMessage.getPreVotes()));
        new ListWriter<>(new SignedVoteScaleWriter()).write(writer, Arrays.asList(catchUpMessage.getPreCommits()));
        writer.writeUint256(catchUpMessage.getBlockHash().getBytes());
        writer.writeUint32(catchUpMessage.getBlockNumber().longValue());
    }
}

package com.limechain.network.protocol.grandpa.messages.catchup.res;

import com.limechain.network.protocol.grandpa.messages.GrandpaMessageType;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import io.emeraldpay.polkaj.scale.ScaleWriter;
import io.emeraldpay.polkaj.scale.writer.ListWriter;
import io.emeraldpay.polkaj.scale.writer.UInt64Writer;

import java.io.IOException;
import java.util.Arrays;

public class CatchUpMessageScaleWriter implements ScaleWriter<CatchUpMessage> {
    private static final CatchUpMessageScaleWriter INSTANCE = new CatchUpMessageScaleWriter();
    
    private final UInt64Writer uint64Writer;
    private final ListWriter<SignedVote> signedVoteListWriter;

    private CatchUpMessageScaleWriter() {
        uint64Writer = new UInt64Writer();
        signedVoteListWriter = new ListWriter<>(SignedVoteScaleWriter.getInstance());
    }

    public static CatchUpMessageScaleWriter getInstance() {
        return INSTANCE;
    }

    @Override
    public void write(ScaleCodecWriter writer, CatchUpMessage catchUpMessage) throws IOException {
        writer.writeByte(GrandpaMessageType.CATCH_UP_RESPONSE.getType());
        uint64Writer.write(writer, catchUpMessage.getSetId());
        uint64Writer.write(writer, catchUpMessage.getRound());
        signedVoteListWriter.write(writer, Arrays.asList(catchUpMessage.getPreVotes()));
        signedVoteListWriter.write(writer, Arrays.asList(catchUpMessage.getPreCommits()));
        writer.writeUint256(catchUpMessage.getBlockHash().getBytes());
        writer.writeUint32(catchUpMessage.getBlockNumber().longValue());
    }
}

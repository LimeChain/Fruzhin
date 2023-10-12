package com.limechain.network.protocol.grandpa.messages.vote;

import com.limechain.network.protocol.grandpa.messages.GrandpaMessageType;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import io.emeraldpay.polkaj.scale.ScaleWriter;
import io.emeraldpay.polkaj.scale.writer.UInt64Writer;

import java.io.IOException;

public class VoteMessageScaleWriter implements ScaleWriter<VoteMessage> {

    private static final VoteMessageScaleWriter INSTANCE = new VoteMessageScaleWriter();

    private final UInt64Writer uint64Writer;
    private final SignedMessageScaleWriter signedMessageScaleWriter;

    private VoteMessageScaleWriter() {
        uint64Writer = new UInt64Writer();
        signedMessageScaleWriter = SignedMessageScaleWriter.getInstance();
    }

    public static VoteMessageScaleWriter getInstance() {
        return INSTANCE;
    }

    @Override
    public void write(ScaleCodecWriter writer, VoteMessage voteMessage) throws IOException {
        writer.writeByte(GrandpaMessageType.VOTE.getType());
        uint64Writer.write(writer, voteMessage.getRound());
        uint64Writer.write(writer, voteMessage.getSetId());
        signedMessageScaleWriter.write(writer, voteMessage.getMessage());
    }

}

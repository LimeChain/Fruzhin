package com.limechain.network.protocol.grandpa.messages.vote;

import com.limechain.network.protocol.grandpa.messages.GrandpaMessageType;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import io.emeraldpay.polkaj.scale.ScaleWriter;
import io.emeraldpay.polkaj.scale.writer.UInt64Writer;

import java.io.IOException;

public class VoteMessageScaleWriter implements ScaleWriter<VoteMessage> {
    @Override
    public void write(ScaleCodecWriter writer, VoteMessage voteMessage) throws IOException {
        writer.writeByte(GrandpaMessageType.VOTE.getType());
        new UInt64Writer().write(writer, voteMessage.getRoundNumber());
        new UInt64Writer().write(writer, voteMessage.getSetId());
        new SignedMessageScaleWriter().write(writer, voteMessage.getMessage());
    }

}

package com.limechain.network.protocol.grandpa.messages.commit;

import com.limechain.network.protocol.grandpa.messages.GrandpaMessageType;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import io.emeraldpay.polkaj.scale.ScaleWriter;
import io.emeraldpay.polkaj.scale.writer.UInt64Writer;

import java.io.IOException;

public class CommitMessageScaleWriter implements ScaleWriter<CommitMessage> {
    @Override
    public void write(ScaleCodecWriter writer, CommitMessage commitMessage) throws IOException {
        writer.writeByte(GrandpaMessageType.COMMIT.getType());
        new UInt64Writer().write(writer, commitMessage.getRoundNumber());
        new UInt64Writer().write(writer, commitMessage.getSetId());
        new VoteScaleWriter().write(writer, commitMessage.getVote());
        new CompactJustificationScaleWriter().write(writer, commitMessage.getPrecommits());
    }
}

package com.limechain.network.protocol.grandpa.messages.commit;

import com.limechain.network.protocol.grandpa.messages.GrandpaMessageType;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleReader;
import io.emeraldpay.polkaj.scale.reader.UInt64Reader;

public class CommitMessageScaleReader implements ScaleReader<CommitMessage> {
    @Override
    public CommitMessage read(ScaleCodecReader reader) {
        int messageType = reader.readByte();
        if (messageType != GrandpaMessageType.COMMIT.getType()) {
            throw new RuntimeException(
                    String.format("Trying to read message of type %d as a commit message", messageType));
        }
        CommitMessage commitMessage = new CommitMessage();
        commitMessage.setRoundNumber(new UInt64Reader().read(reader));
        commitMessage.setSetId(new UInt64Reader().read(reader));
        commitMessage.setVote(VoteScaleReader.getInstance().read(reader));
        commitMessage.setPrecommits(new CompactJustificationScaleReader().read(reader));
        return commitMessage;
    }
}

package com.limechain.network.protocol.grandpa.messages.commit;

import com.limechain.network.protocol.grandpa.messages.GrandpaMessageType;
import com.limechain.network.protocol.warp.dto.Precommit;
import com.limechain.network.protocol.warp.scale.PrecommitReader;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleReader;
import io.emeraldpay.polkaj.scale.reader.UInt64Reader;

public class CommitMessageScaleReader implements ScaleReader<CommitMessage> {
    @Override
    public CommitMessage read(ScaleCodecReader reader) {
        int messageType = reader.readByte();
        if (messageType != GrandpaMessageType.COMMIT.getType()) {
            return null;
        }
        CommitMessage commitMessage = new CommitMessage();
        commitMessage.setRoundNumber(new UInt64Reader().read(reader));
        commitMessage.setSetId(new UInt64Reader().read(reader));
        commitMessage.setVote(new VoteScaleReader().read(reader));

        //TODO: check parsing after we start to receive more than 1 authority
        int precommitsCount = reader.readCompactInt();
        Precommit[] precommits = new Precommit[precommitsCount];
        PrecommitReader precommitReader = new PrecommitReader();
        for (int i = 0; i < precommitsCount; i++) {
            precommits[i] = precommitReader.read(reader);
        }
        commitMessage.setPrecommits(precommits);
        return commitMessage;
    }
}

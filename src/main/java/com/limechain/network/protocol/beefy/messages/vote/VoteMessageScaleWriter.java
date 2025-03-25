package com.limechain.network.protocol.beefy.messages.vote;

import com.limechain.consensus.beefy.scale.CommitmentScaleWriter;
import com.limechain.network.protocol.beefy.messages.BeefyMessageType;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import io.emeraldpay.polkaj.scale.ScaleWriter;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.IOException;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class VoteMessageScaleWriter implements ScaleWriter<VoteMessage> {

    private static final VoteMessageScaleWriter INSTANCE = new VoteMessageScaleWriter();

    public static VoteMessageScaleWriter getInstance() {
        return INSTANCE;
    }

    @Override
    public void write(ScaleCodecWriter writer, VoteMessage voteMessage) throws IOException {

        writer.writeByte(BeefyMessageType.VOTE.getType());
        writer.write(CommitmentScaleWriter.getInstance(), voteMessage.getCommitment());
        writer.writeByteArray(voteMessage.getAuthorityId());
        writer.writeByteArray(voteMessage.getSignature());
    }
}

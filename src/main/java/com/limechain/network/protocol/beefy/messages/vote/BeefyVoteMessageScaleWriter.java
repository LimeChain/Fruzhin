package com.limechain.network.protocol.beefy.messages.vote;

import com.limechain.consensus.beefy.scale.CommitmentScaleWriter;
import com.limechain.network.protocol.beefy.messages.BeefyMessageType;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import io.emeraldpay.polkaj.scale.ScaleWriter;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.io.IOException;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BeefyVoteMessageScaleWriter implements ScaleWriter<BeefyVoteMessage> {

    private static final BeefyVoteMessageScaleWriter INSTANCE = new BeefyVoteMessageScaleWriter();

    public static BeefyVoteMessageScaleWriter getInstance() {
        return INSTANCE;
    }

    @Override
    public void write(ScaleCodecWriter writer, BeefyVoteMessage beefyVoteMessage) throws IOException {

        writer.writeByte(BeefyMessageType.VOTE.getType());
        writer.write(CommitmentScaleWriter.getInstance(), beefyVoteMessage.getCommitment());
        writer.writeByteArray(beefyVoteMessage.getAuthorityId());
        writer.writeByteArray(beefyVoteMessage.getSignature());
    }
}

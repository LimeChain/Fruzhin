package com.limechain.network.protocol.grandpa.messages.catchup.res;

import com.limechain.network.protocol.grandpa.messages.commit.VoteScaleWriter;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import io.emeraldpay.polkaj.scale.ScaleWriter;

import java.io.IOException;

public class SignedVoteScaleWriter implements ScaleWriter<SignedVote> {

    @Override
    public void write(ScaleCodecWriter writer, SignedVote signedVote) throws IOException {
        VoteScaleWriter voteScaleWriter = new VoteScaleWriter();

        voteScaleWriter.write(writer, signedVote.getVote());
        writer.writeByteArray(signedVote.getSignature().getBytes());
        writer.writeUint256(signedVote.getAuthorityPublicKey().getBytes());
    }
}

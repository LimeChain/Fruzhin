package com.limechain.network.protocol.grandpa.messages.catchup.res;

import com.limechain.network.protocol.grandpa.messages.commit.VoteScaleWriter;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import io.emeraldpay.polkaj.scale.ScaleWriter;

import java.io.IOException;

public class SignedVoteScaleWriter implements ScaleWriter<SignedVote[]> {

    @Override
    public void write(ScaleCodecWriter writer, SignedVote[] signoedVotes) throws IOException {
        writer.writeCompact(signoedVotes.length);
        VoteScaleWriter voteScaleWriter = new VoteScaleWriter();

        for (int i = 0; i < signoedVotes.length; i++) {
            SignedVote signedVote = signoedVotes[i];
            voteScaleWriter.write(writer, signedVote.getVote());
            writer.writeByteArray(signedVote.getSignature().getBytes());
            writer.writeUint256(signedVote.getAuthorityPublicKey().getBytes());
        }
    }
}

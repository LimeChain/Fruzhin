package com.limechain.consensus.beefy.scale.runtime;

import com.limechain.consensus.beefy.dto.DoubleVotingProof;
import com.limechain.network.protocol.beefy.messages.vote.VoteMessageScaleWriter;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import io.emeraldpay.polkaj.scale.ScaleWriter;

import java.io.IOException;

public class BeefyDoubleVotingProofScaleWriter implements ScaleWriter<DoubleVotingProof> {

    private static final BeefyDoubleVotingProofScaleWriter INSTANCE = new BeefyDoubleVotingProofScaleWriter();

    private BeefyDoubleVotingProofScaleWriter() {
    }

    public static BeefyDoubleVotingProofScaleWriter getInstance() {
        return INSTANCE;
    }

    @Override
    public void write(ScaleCodecWriter writer, DoubleVotingProof doubleVotingProof) throws IOException {
        writer.write(VoteMessageScaleWriter.getInstance(), doubleVotingProof.getFirst());
        writer.write(VoteMessageScaleWriter.getInstance(), doubleVotingProof.getSecond());
    }
}
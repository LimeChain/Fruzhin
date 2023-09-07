package com.limechain.network.protocol.grandpa.messages.commit;

import com.limechain.network.protocol.warp.dto.Precommit;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import io.emeraldpay.polkaj.scale.ScaleWriter;

import java.io.IOException;

public class CompactJustificationScaleWriter implements ScaleWriter<Precommit[]> {

    @Override
    public void write(ScaleCodecWriter writer, Precommit[] precommits) throws IOException {

        writer.writeCompact(precommits.length);
        VoteScaleWriter voteScaleWriter = new VoteScaleWriter();

        for (int i = 0; i < precommits.length; i++){
            Precommit precommit = precommits[i];
            Vote vote = new Vote();
            vote.setBlockHash(precommit.getTargetHash());
            vote.setBlockNumber(precommit.getTargetNumber());
            voteScaleWriter.write(writer, vote);
        }

        writer.writeCompact(precommits.length);

        for (int i = 0; i < precommits.length; i++) {
            Precommit precommit = precommits[i];
            writer.writeByteArray(precommit.getSignature().getBytes());
            writer.writeUint256(precommit.getAuthorityPublicKey().getBytes());
        }
    }
}

package com.limechain.network.protocol.grandpa.messages.commit;

import com.limechain.network.protocol.warp.dto.Precommit;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import io.emeraldpay.polkaj.scale.ScaleWriter;

import java.io.IOException;

public class CompactJustificationScaleWriter implements ScaleWriter<Precommit[]> {

    private static final CompactJustificationScaleWriter INSTANCE = new CompactJustificationScaleWriter();

    private final VoteScaleWriter voteScaleWriter;

    private CompactJustificationScaleWriter() {
        voteScaleWriter = VoteScaleWriter.getInstance();
    }

    public static CompactJustificationScaleWriter getInstance() {
        return INSTANCE;
    }

    @Override
    public void write(ScaleCodecWriter writer, Precommit[] precommits) throws IOException {
        writer.writeCompact(precommits.length);

        for (int i = 0; i < precommits.length; i++) {
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

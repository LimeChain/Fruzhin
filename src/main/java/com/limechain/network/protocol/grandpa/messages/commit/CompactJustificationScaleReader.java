package com.limechain.network.protocol.grandpa.messages.commit;

import com.limechain.network.protocol.warp.dto.Precommit;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleReader;
import io.emeraldpay.polkaj.scale.reader.ListReader;
import io.emeraldpay.polkaj.types.Hash256;
import io.emeraldpay.polkaj.types.Hash512;

import java.util.List;

public class CompactJustificationScaleReader implements ScaleReader<Precommit[]> {

    private static final CompactJustificationScaleReader INSTANCE = new CompactJustificationScaleReader();

    private final ListReader<Vote> voteListReader;

    private CompactJustificationScaleReader() {
        VoteScaleReader voteScaleReader = VoteScaleReader.getInstance();
        voteListReader = new ListReader<>(voteScaleReader);
    }

    public static CompactJustificationScaleReader getInstance() {
        return INSTANCE;
    }

    @Override
    public Precommit[] read(ScaleCodecReader reader) {
        List<Vote> votes = voteListReader.read(reader);

        int precommitsCount = votes.size();
        Precommit[] precommits = new Precommit[precommitsCount];

        for (int i = 0; i < precommitsCount; i++) {
            Vote vote = votes.get(i);
            precommits[i] = new Precommit();
            precommits[i].setTargetHash(vote.getBlockHash());
            precommits[i].setTargetNumber(vote.getBlockNumber());
        }

        int signaturesCount = reader.readCompactInt();
        if (signaturesCount != precommitsCount) {
            throw new RuntimeException("Number of signatures does not match number of precommits");
        }

        for (int i = 0; i < signaturesCount; i++) {
            precommits[i].setSignature(new Hash512(reader.readByteArray(64)));
            precommits[i].setAuthorityPublicKey(new Hash256(reader.readUint256()));
        }

        return precommits;
    }
}

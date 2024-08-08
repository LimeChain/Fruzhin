package com.limechain.network.protocol.grandpa.messages.commit;

import com.limechain.exception.network.SignatureCountMismatchException;
import com.limechain.network.protocol.warp.dto.Precommit;
import com.limechain.polkaj.Hash256;
import com.limechain.polkaj.Hash512;
import com.limechain.polkaj.reader.ListReader;
import com.limechain.polkaj.reader.ScaleCodecReader;
import com.limechain.polkaj.reader.ScaleReader;

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
            throw new SignatureCountMismatchException(
                    String.format("Number of signatures (%d) does not match number of precommits (%d)",
                            signaturesCount, precommitsCount));
        }

        for (int i = 0; i < signaturesCount; i++) {
            precommits[i].setSignature(new Hash512(reader.readByteArray(64)));
            precommits[i].setAuthorityPublicKey(new Hash256(reader.readUint256()));
        }

        return precommits;
    }
}

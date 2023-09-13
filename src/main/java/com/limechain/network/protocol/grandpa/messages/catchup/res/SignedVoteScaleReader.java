package com.limechain.network.protocol.grandpa.messages.catchup.res;

import com.limechain.network.protocol.grandpa.messages.commit.VoteScaleReader;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleReader;
import io.emeraldpay.polkaj.types.Hash256;
import io.emeraldpay.polkaj.types.Hash512;

public class SignedVoteScaleReader implements ScaleReader<SignedVote[]> {

    @Override
    public SignedVote[] read(ScaleCodecReader reader) {
        VoteScaleReader voteScaleReader = new VoteScaleReader();
        int votesCount = reader.readCompactInt();
        SignedVote[] signedVote = new SignedVote[votesCount];

        for (int i = 0; i < votesCount; i++) {
            signedVote[i] = new SignedVote();
            signedVote[i].setVote(voteScaleReader.read(reader));
            signedVote[i].setSignature(new Hash512(reader.readByteArray(64)));
            signedVote[i].setAuthorityPublicKey(new Hash256(reader.readUint256()));
        }

        return signedVote;
    }
}

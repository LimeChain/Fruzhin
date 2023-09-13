package com.limechain.network.protocol.grandpa.messages.catchup.res;

import com.limechain.network.protocol.grandpa.messages.commit.VoteScaleReader;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleReader;
import io.emeraldpay.polkaj.types.Hash256;
import io.emeraldpay.polkaj.types.Hash512;

public class SignedVoteScaleReader implements ScaleReader<SignedVote> {

    @Override
    public SignedVote read(ScaleCodecReader reader) {
        VoteScaleReader voteScaleReader = new VoteScaleReader();

        SignedVote signedVote = new SignedVote();
        signedVote.setVote(voteScaleReader.read(reader));
        signedVote.setSignature(new Hash512(reader.readByteArray(64)));
        signedVote.setAuthorityPublicKey(new Hash256(reader.readUint256()));

        return signedVote;
    }
}

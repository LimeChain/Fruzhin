package com.limechain.network.protocol.beefy.messages.vote;

import com.limechain.consensus.beefy.dto.Commitment;
import com.limechain.network.protocol.beefy.messages.justification.CommitmentScaleReader;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleReader;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class VoteMessageScaleReader implements ScaleReader<VoteMessage> {
    public static final int ECDSA_PUBLIC_KEY_LENGTH = 33;
    public static final int ECDSA_SIGNATURE_LENGTH = 65;

    private static final VoteMessageScaleReader INSTANCE = new VoteMessageScaleReader();

    public static VoteMessageScaleReader getInstance() {
        return INSTANCE;
    }

    @Override
    public VoteMessage read(ScaleCodecReader reader) {
        Commitment commitment = reader.read(CommitmentScaleReader.getInstance());
        byte[] authorityId = reader.readByteArray(ECDSA_PUBLIC_KEY_LENGTH);
        byte[] signature = reader.readByteArray(ECDSA_SIGNATURE_LENGTH);

        return new VoteMessage(commitment, authorityId, signature);
    }
}

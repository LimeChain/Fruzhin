package com.limechain.network.protocol.beefy.messages.justification;

import com.limechain.consensus.beefy.dto.Commitment;
import com.limechain.consensus.beefy.scale.CommitmentScaleReader;
import com.limechain.utils.EcdsaUtils;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleReader;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SignedCommitmentScaleReader implements ScaleReader<SignedCommitment> {

    private static final SignedCommitmentScaleReader INSTANCE = new SignedCommitmentScaleReader();

    public static SignedCommitmentScaleReader getInstance() {
        return INSTANCE;
    }

    @Override
    public SignedCommitment read(ScaleCodecReader reader) {
        Commitment commitment = reader.read(CommitmentScaleReader.getInstance());

        int size = reader.readCompactInt();

        List<Optional<byte[]>> signatures = new ArrayList<>();
        for (int i = 0; i < size; ++i) {
            signatures.add(Optional.ofNullable(reader.readByteArray(EcdsaUtils.SIGNATURE_LEN)));
        }

        return new SignedCommitment(commitment, signatures);
    }
}

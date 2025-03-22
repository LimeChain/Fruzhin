package com.limechain.consensus.beefy.scale;

import com.limechain.consensus.beefy.dto.BeefyPayloadId;
import com.limechain.consensus.beefy.dto.Commitment;
import com.limechain.consensus.beefy.dto.PayloadElement;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleReader;
import io.emeraldpay.polkaj.scale.reader.UInt64Reader;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CommitmentScaleReader implements ScaleReader<Commitment> {

    private static final int BEEFY_PAYLOAD_ID_LENGTH = 2;

    private static final CommitmentScaleReader INSTANCE = new CommitmentScaleReader();

    public static CommitmentScaleReader getInstance() {
        return INSTANCE;
    }

    @Override
    public Commitment read(ScaleCodecReader reader) {

        int payloadCount = reader.readCompactInt();
        List<PayloadElement> payloadElements = new ArrayList<>();

        for (int i = 0; i < payloadCount; ++i) {

            BeefyPayloadId id = BeefyPayloadId.fromBytes(reader.readByteArray(BEEFY_PAYLOAD_ID_LENGTH));

            int payloadLength = reader.readCompactInt();
            byte[] payloadBytes = reader.readByteArray(payloadLength);

            payloadElements.add(new PayloadElement(id, payloadBytes));
        }

        BigInteger blockNumber = BigInteger.valueOf(reader.readUint32());
        BigInteger authoritySetId = new UInt64Reader().read(reader);

        return new Commitment(payloadElements, blockNumber, authoritySetId);
    }
}

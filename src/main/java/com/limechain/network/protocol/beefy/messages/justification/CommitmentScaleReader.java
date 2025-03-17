package com.limechain.network.protocol.beefy.messages.justification;

import com.limechain.consensus.beefy.dto.Commitment;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleReader;
import io.emeraldpay.polkaj.scale.reader.UInt64Reader;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CommitmentScaleReader implements ScaleReader<Commitment> {

    private static final CommitmentScaleReader INSTANCE = new CommitmentScaleReader();

    public static CommitmentScaleReader getInstance() {
        return INSTANCE;
    }

    @Override
    public Commitment read(ScaleCodecReader reader) {

        int size = reader.readCompactInt();
//        List<PayloadElement> result = new ArrayList(size);
//
//        for(int i = 0; i < size; ++i) {
//            result.add(reader.read(PayloadElementScaleReader.getInstance()));
//        }

        byte[] mmr = reader.readUint256();

        BigInteger blockNumber = BigInteger.valueOf(reader.readUint32());
        BigInteger authoritySetId = new UInt64Reader().read(reader);

//        return new Commitment(result, blockNumber, authoritySetId);
        return null;
    }
}

package com.limechain.network.protocol.beefy.messages.justification;

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

import static com.limechain.utils.StringUtils.hexToBytes;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class CommitmentScaleReader implements ScaleReader<Commitment> {

    private static final CommitmentScaleReader INSTANCE = new CommitmentScaleReader();

    public static CommitmentScaleReader getInstance() {
        return INSTANCE;
    }

    @Override
    public Commitment read(ScaleCodecReader reader) {
        String hex = "046d68343048656c6c6f20576f726c6421050000000000000000000000000000000000000000000000";

        byte[] bytes = hexToBytes(hex);
        ScaleCodecReader reader1 = new ScaleCodecReader(new byte[]{0x04});  // Encoded `1`
        Integer decodedValue = reader1.readCompactInt();

//        reader.skip(1);
        int size = reader.readCompactInt();
        List<PayloadElement> result = new ArrayList<>();

        for (int i = 0; i < size; ++i) {
            result.add(reader.read(PayloadElementScaleReader.getInstance()));
        }

//        byte[] mmr = reader.readUint256();

        BigInteger blockNumber = BigInteger.valueOf(reader.readUint32());
        BigInteger authoritySetId = new UInt64Reader().read(reader);

        return new Commitment(result, blockNumber, authoritySetId);
//        return null;
    }
}

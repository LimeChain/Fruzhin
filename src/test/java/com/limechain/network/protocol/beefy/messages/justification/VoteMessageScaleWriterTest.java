package com.limechain.network.protocol.beefy.messages.justification;

import com.limechain.consensus.beefy.dto.BeefyPayloadId;
import com.limechain.consensus.beefy.dto.Commitment;
import com.limechain.consensus.beefy.dto.PayloadElement;
import com.limechain.utils.StringUtils;
import com.limechain.utils.scale.ScaleUtils;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class VoteMessageScaleWriterTest {

    private static final String SCALE_ENCODED_JUST = "0x0101046d6880d7a4152645a6b797e86a8c697cc4744c9fb37af3103adc2b7" +
            "d09418e77c6c5b099b2a501222f000000000000046005000000080b539afa97682ef165a5a9882677c576b846eb14cff6a07ce64" +
            "8a17c5f0be15f7e8d0b234d2d358b32cde75310ff7c839c46f12b0da23864a8c1379872d21157000a90d82cdf8ac5c6ae22bb9c9" +
            "3d2452dad45e59092baa39b49b361cf8d62f4e04b778fb25abce3778b1b0f9ff7d3190e6e534c9b071d54092f7c932b98632c6e00";

    private static final BigInteger BLOCK_NUMBER = BigInteger.valueOf(27636377);
    private static final BigInteger AUTHORITY_SET_ID = BigInteger.valueOf(12066);
    private static final int SIGNATURE_SIZE = 5;

    private final SignedCommitmentScaleWriter writer = SignedCommitmentScaleWriter.getInstance();

    @Test
    void testWrite() {
        byte[] payloadMmr = new byte[]{-41, -92, 21, 38, 69, -90, -73, -105, -24, 106, -116, 105, 124, -60, 116, 76,
                -97, -77, 122, -13, 16, 58, -36, 43, 125, 9, 65, -114, 119, -58, -59, -80};
        PayloadElement payloadElement = new PayloadElement(BeefyPayloadId.MMR, payloadMmr);
        Commitment commitment = new Commitment(List.of(payloadElement), BLOCK_NUMBER, AUTHORITY_SET_ID);

        SignedCommitment voteMessage = getSignedCommitment(commitment);

        byte[] encoded = ScaleUtils.Encode.encode(writer, voteMessage);

        assertNotNull(encoded);
        assertEquals(SCALE_ENCODED_JUST, StringUtils.toHexWithPrefix(encoded));
    }

    private static SignedCommitment getSignedCommitment(Commitment commitment) {
        Optional<byte[]>[] signatures = new Optional[SIGNATURE_SIZE];
        Arrays.fill(signatures, Optional.empty());

        byte[] signature1 = new byte[]{11, 83, -102, -6, -105, 104, 46, -15, 101, -91, -87, -120, 38, 119, -59,
                118, -72, 70, -21, 20, -49, -10, -96, 124, -26, 72, -95, 124, 95, 11, -31, 95, 126, -115, 11, 35, 77,
                45, 53, -117, 50, -51, -25, 83, 16, -1, 124, -125, -100, 70, -15, 43, 13, -94, 56, 100, -88, -63, 55,
                -104, 114, -46, 17, 87, 0};
        byte[] signature2 = new byte[]{10, -112, -40, 44, -33, -118, -59, -58, -82, 34, -69, -100, -109, -46, 69, 45,
                -83, 69, -27, -112, -110, -70, -93, -101, 73, -77, 97, -49, -115, 98, -12, -32, 75, 119, -113, -78,
                90, -68, -29, 119, -117, 27, 15, -97, -9, -45, 25, 14, 110, 83, 76, -101, 7, 29, 84, 9, 47, 124,
                -109, 43, -104, 99, 44, 110, 0};

        signatures[1] = Optional.of(signature1);
        signatures[2] = Optional.of(signature2);

        return new SignedCommitment(commitment, Arrays.asList(signatures));
    }
}

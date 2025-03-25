package com.limechain.network.protocol.beefy.messages.justification;

import com.limechain.consensus.beefy.dto.BeefyPayloadId;
import com.limechain.consensus.beefy.dto.Commitment;
import com.limechain.consensus.beefy.dto.PayloadElement;
import com.limechain.utils.StringUtils;
import com.limechain.utils.scale.ScaleUtils;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SignedCommitmentScaleReaderTest {

    private static final String SCALE_ENCODED_JUST = "0x0101046d6880d7a4152645a6b797e86a8c697cc4744c9fb37af3103adc2b7" +
            "d09418e77c6c5b099b2a501222f000000000000046005000000080b539afa97682ef165a5a9882677c576b846eb14cff6a07ce64" +
            "8a17c5f0be15f7e8d0b234d2d358b32cde75310ff7c839c46f12b0da23864a8c1379872d21157000a90d82cdf8ac5c6ae22bb9c9" +
            "3d2452dad45e59092baa39b49b361cf8d62f4e04b778fb25abce3778b1b0f9ff7d3190e6e534c9b071d54092f7c932b98632c6e00";

    private static final BigInteger BLOCK_NUMBER = BigInteger.valueOf(27636377);
    private static final BigInteger AUTHORITY_SET_ID = BigInteger.valueOf(12066);

    private final SignedCommitmentScaleReader reader = SignedCommitmentScaleReader.getInstance();

    @Test
    void testRead() {
        byte[] testData = StringUtils.hexToBytes(SCALE_ENCODED_JUST);

        SignedCommitment decoded = ScaleUtils.Decode.decode(testData, reader);

        assertNotNull(decoded);

        Commitment decodedCommitment = decoded.getCommitment();
        byte[] payloadMmr = new byte[]{-41, -92, 21, 38, 69, -90, -73, -105, -24, 106, -116, 105, 124, -60, 116, 76,
                -97, -77, 122, -13, 16, 58, -36, 43, 125, 9, 65, -114, 119, -58, -59, -80};
        PayloadElement payloadElement = new PayloadElement(BeefyPayloadId.MMR, payloadMmr);
        Commitment commitment = new Commitment(List.of(payloadElement), BLOCK_NUMBER, AUTHORITY_SET_ID);
        assertEquals(commitment, decodedCommitment);

        assertEquals(BLOCK_NUMBER, decodedCommitment.getBlockNumber());
        assertEquals(AUTHORITY_SET_ID, decodedCommitment.getAuthoritySetId());

        assertEquals(5, decoded.getSignatures().size());
        assertEquals(2, decoded.getSignatures().stream()
                .filter(Optional::isPresent)
                .count());
    }
}

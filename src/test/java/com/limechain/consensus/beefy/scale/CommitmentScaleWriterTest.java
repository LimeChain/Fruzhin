package com.limechain.consensus.beefy.scale;

import com.limechain.consensus.beefy.dto.BeefyPayloadId;
import com.limechain.consensus.beefy.dto.Commitment;
import com.limechain.consensus.beefy.dto.PayloadElement;
import com.limechain.utils.StringUtils;
import com.limechain.utils.scale.ScaleUtils;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CommitmentScaleWriterTest {

    private static final String SCALE_ENCODED_COMMITMENT = "0x046d68802929adcd6f4f60f87f9090382fe842d1dddd7cb6b338f" +
            "d0e35c42bf8240cc6ab4abaa401b62e000000000000";

    private static final BigInteger BLOCK_NUMBER = BigInteger.valueOf(27572810);
    private static final BigInteger AUTHORITY_SET_ID = BigInteger.valueOf(11958);

    private final CommitmentScaleWriter writer = CommitmentScaleWriter.getInstance();

    @Test
    void testWrite() {
        byte[] payloadMmr = new byte[]{41, 41, -83, -51, 111, 79, 96, -8, 127, -112, -112, 56, 47, -24, 66,
                -47, -35, -35, 124, -74, -77, 56, -3, 14, 53, -60, 43, -8, 36, 12, -58, -85};
        PayloadElement payloadElement = new PayloadElement(BeefyPayloadId.MMR, payloadMmr);
        Commitment commitment = new Commitment(List.of(payloadElement), BLOCK_NUMBER, AUTHORITY_SET_ID);

        byte[] encoded = ScaleUtils.Encode.encode(writer, commitment);

        assertNotNull(encoded);
        assertEquals(SCALE_ENCODED_COMMITMENT, StringUtils.toHexWithPrefix(encoded));
    }
}

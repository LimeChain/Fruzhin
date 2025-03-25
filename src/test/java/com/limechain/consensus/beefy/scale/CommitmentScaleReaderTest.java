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

class CommitmentScaleReaderTest {

    private static final String SCALE_ENCODED_COMMITMENT = "0x046d68802929adcd6f4f60f87f9090382fe842d1dddd7cb6b338fd0e35" +
            "c42bf8240cc6ab4abaa401b62e00000000000002";

    private static final BigInteger BLOCK_NUMBER = BigInteger.valueOf(27572810);
    private static final BigInteger AUTHORITY_SET_ID = BigInteger.valueOf(11958);

    private final CommitmentScaleReader reader = CommitmentScaleReader.getInstance();

    @Test
    void testRead() {
        byte[] testData = StringUtils.hexToBytes(SCALE_ENCODED_COMMITMENT);

        Commitment decoded = ScaleUtils.Decode.decode(testData, reader);

        assertNotNull(decoded);

        List<PayloadElement> payloads = decoded.getPayload();
        assertEquals(1, payloads.size());

        PayloadElement payloadElement = payloads.getFirst();
        assertEquals(BeefyPayloadId.MMR, payloadElement.getPayloadId());
        assertEquals(32, payloadElement.getData().length);

        assertEquals(BLOCK_NUMBER, decoded.getBlockNumber());
        assertEquals(AUTHORITY_SET_ID, decoded.getAuthoritySetId());
    }
}

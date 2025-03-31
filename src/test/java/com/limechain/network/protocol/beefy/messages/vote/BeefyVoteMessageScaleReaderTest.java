package com.limechain.network.protocol.beefy.messages.vote;

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

class BeefyVoteMessageScaleReaderTest {

    private static final String SCALE_ENCODED_VOTE = "0x00046d68802929adcd6f4f60f87f9090382fe842d1dddd7cb6b338fd0e35" +
            "c42bf8240cc6ab4abaa401b62e000000000000024b1b43b9ddd8a8abf9bd503bf9b42602a7ddae6941a6c904b8e76b7a3d6331" +
            "dac52804f9a242093d0bc43e87ec2b7fc028359668fd4c8478954223ab7a7f66b26eb3a67c9d07d7507536bbab2618e49cf39" +
            "425c172d4c0e7d0a04e7ebd61b45f01";

    private static final BigInteger BLOCK_NUMBER = BigInteger.valueOf(27572810);
    private static final BigInteger AUTHORITY_SET_ID = BigInteger.valueOf(11958);

    private final BeefyVoteMessageScaleReader reader = BeefyVoteMessageScaleReader.getInstance();

    @Test
    void testRead() {
        byte[] testData = StringUtils.hexToBytes(SCALE_ENCODED_VOTE);

        BeefyVoteMessage decoded = ScaleUtils.Decode.decode(testData, reader);

        assertNotNull(decoded);

        Commitment decodedCommitment = decoded.getCommitment();
        byte[] payloadMmr = new byte[]{41, 41, -83, -51, 111, 79, 96, -8, 127, -112, -112, 56, 47, -24, 66,
                -47, -35, -35, 124, -74, -77, 56, -3, 14, 53, -60, 43, -8, 36, 12, -58, -85};
        PayloadElement payloadElement = new PayloadElement(BeefyPayloadId.MMR, payloadMmr);
        Commitment commitment = new Commitment(List.of(payloadElement), BLOCK_NUMBER, AUTHORITY_SET_ID);
        assertEquals(commitment, decodedCommitment);

        assertEquals(BLOCK_NUMBER, decodedCommitment.getBlockNumber());
        assertEquals(AUTHORITY_SET_ID, decodedCommitment.getAuthoritySetId());

        assertEquals(33, decoded.getAuthorityId().length);
        assertEquals(65, decoded.getSignature().length);
    }
}

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

class VoteMessageScaleWriterTest {

    private static final String SCALE_ENCODED_VOTE = "0x00046d68802929adcd6f4f60f87f9090382fe842d1dddd7cb6b338fd0e35" +
            "c42bf8240cc6ab4abaa401b62e000000000000024b1b43b9ddd8a8abf9bd503bf9b42602a7ddae6941a6c904b8e76b7a3d6331" +
            "dac52804f9a242093d0bc43e87ec2b7fc028359668fd4c8478954223ab7a7f66b26eb3a67c9d07d7507536bbab2618e49cf39" +
            "425c172d4c0e7d0a04e7ebd61b45f01";

    private static final BigInteger BLOCK_NUMBER = BigInteger.valueOf(27572810);
    private static final BigInteger AUTHORITY_SET_ID = BigInteger.valueOf(11958);

    private final VoteMessageScaleWriter writer = VoteMessageScaleWriter.getInstance();

    @Test
    void testWrite() {
        byte[] payloadMmr = new byte[]{41, 41, -83, -51, 111, 79, 96, -8, 127, -112, -112, 56, 47, -24, 66,
                -47, -35, -35, 124, -74, -77, 56, -3, 14, 53, -60, 43, -8, 36, 12, -58, -85};
        PayloadElement payloadElement = new PayloadElement(BeefyPayloadId.MMR, payloadMmr);
        Commitment commitment = new Commitment(List.of(payloadElement), BLOCK_NUMBER, AUTHORITY_SET_ID);

        VoteMessage voteMessage = getVoteMessage(commitment);

        byte[] encoded = ScaleUtils.Encode.encode(writer, voteMessage);

        assertNotNull(encoded);
        assertEquals(SCALE_ENCODED_VOTE, StringUtils.toHexWithPrefix(encoded));
    }

    private static VoteMessage getVoteMessage(Commitment commitment) {
        byte[] authoritySetId = new byte[]{2, 75, 27, 67, -71, -35, -40, -88, -85, -7, -67, 80, 59, -7, -76, 38,
                2, -89, -35, -82, 105, 65, -90, -55, 4, -72, -25, 107, 122, 61, 99, 49, -38};
        byte[] signature = new byte[]{-59, 40, 4, -7, -94, 66, 9, 61, 11, -60, 62, -121, -20, 43, 127, -64, 40, 53,
                -106, 104, -3, 76, -124, 120, -107, 66, 35, -85, 122, 127, 102, -78, 110, -77, -90, 124, -99, 7, -41,
                80, 117, 54, -69, -85, 38, 24, -28, -100, -13, -108, 37, -63, 114, -44, -64, -25, -48, -96, 78, 126,
                -67, 97, -76, 95, 1};

        return new VoteMessage(commitment, authoritySetId, signature);
    }
}

package com.limechain.network.protocol.grandpa.messages.catchup.req;

import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import org.apache.tomcat.util.buf.HexUtils;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CatchupRequestMessageScaleReaderTest {

    //CHECKSTYLE.OFF
    byte[] expectedByteArr = HexUtils.fromHexString("0314000000000000002800000000000000");
    private final CatchUpReqMessage expectedCatchupReqMsg = new CatchUpReqMessage(
            BigInteger.valueOf(20),
            BigInteger.valueOf(40)
    );
    //CHECKSTYLE.ON

    @Test
    void decodeEqualsTest() {
        ScaleCodecReader reader = new ScaleCodecReader(expectedByteArr);
        CatchUpReqMessage commitMessage = reader.read(new CatchUpReqMessageScaleReader());

        assertEquals(expectedCatchupReqMsg, commitMessage);
    }

    @Test
    void encodeEqualsTest() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        new CatchUpReqMessageScaleWriter().write(new ScaleCodecWriter(baos), expectedCatchupReqMsg);

        assertArrayEquals(expectedByteArr, baos.toByteArray());
    }

}

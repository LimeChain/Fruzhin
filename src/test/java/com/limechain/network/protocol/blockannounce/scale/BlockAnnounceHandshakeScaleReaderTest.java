package com.limechain.network.protocol.blockannounce.scale;

import com.google.protobuf.ByteString;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import io.emeraldpay.polkaj.types.Hash256;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlockAnnounceHandshakeScaleReaderTest {

    @Test
    void decodeHandshake() {
        //CHECKSTYLE.OFF
        ByteString byteString = ByteString.fromHex("044d00000001000000000000000000000000000000000000000000000000000000000000000200000000000000000000000000000000000000000000000000000000000000");
        //CHECKSTYLE.ON
        byte[] encodedBytes = byteString.toByteArray();

        ScaleCodecReader reader = new ScaleCodecReader(encodedBytes);
        BlockAnnounceHandShake decoded = reader.read(new BlockAnnounceHandshakeScaleReader());

        assertEquals(4, decoded.getNodeRole());
        assertEquals("77", decoded.getBestBlock());
        assertEquals("0x0100000000000000000000000000000000000000000000000000000000000000",
                decoded.getBestBlockHash().toString());
        assertEquals("0x0200000000000000000000000000000000000000000000000000000000000000",
                decoded.getGenesisBlockHash().toString());
    }

    @Test
    public void EncodeAnnouncementHandshakeTest() {
        //CHECKSTYLE.OFF
        byte[] expected = ByteString.fromHex("044d00000001000000000000000000000000000000000000000000000000000000000000000200000000000000000000000000000000000000000000000000000000000000").toByteArray();
        //CHECKSTYLE.ON

        BlockAnnounceHandShake dataToEncode = new BlockAnnounceHandShake();
        dataToEncode.nodeRole = 4;
        dataToEncode.bestBlock = "77";
        dataToEncode.bestBlockHash = Hash256.from(
                "0x0100000000000000000000000000000000000000000000000000000000000000");
        dataToEncode.genesisBlockHash = Hash256.from(
                "0x0200000000000000000000000000000000000000000000000000000000000000");

        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        try (ScaleCodecWriter writer = new ScaleCodecWriter(buf)) {
            writer.write(new BlockAnnounceHandshakeScaleWriter(), dataToEncode);
            byte[] encoded = buf.toByteArray();

            assertTrue(Arrays.equals(encoded, expected));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
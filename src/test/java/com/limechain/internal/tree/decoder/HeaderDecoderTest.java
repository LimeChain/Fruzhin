package com.limechain.internal.tree.decoder;

import com.limechain.internal.NodeVariant;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class HeaderDecoderTest {
    @Test
    public void decodeKeyTest() throws TrieDecoderException, IOException {
        byte[] expectedPartialKey = new byte[]{9};
        OutputStream out = new ByteArrayOutputStream();
        ScaleCodecWriter writer = new ScaleCodecWriter(out);
        writer.writeByteArray(expectedPartialKey);

        byte[] data = ((ByteArrayOutputStream) out).toByteArray();

        ScaleCodecReader reader = new ScaleCodecReader(data);
        byte[] partialKey = HeaderDecoder.decodeKey(reader, 1);
        assertArrayEquals(expectedPartialKey, partialKey);
    }

    @Test
    public void decodeHeaderByteTest() throws IOException, TrieDecoderException {
        int expectedPartialKeyLength = 1;
        byte[] expectedPartialKey = new byte[]{9};
        byte[] expectedStorageValue = new byte[]{1, 2, 3};
        int nodeType = NodeVariant.LEAF.bits | 1;

        OutputStream out = new ByteArrayOutputStream();
        ScaleCodecWriter writer = new ScaleCodecWriter(out);
        writer.writeByte(nodeType);
        writer.writeAsList(expectedPartialKey);
        writer.writeAsList(expectedStorageValue);

        byte[] data = ((ByteArrayOutputStream) out).toByteArray();
        ScaleCodecReader reader = new ScaleCodecReader(data);
        HeaderDecoder headerResult = HeaderDecoder.decodeHeaderByte(reader.readByte());

        assertEquals(expectedPartialKeyLength, headerResult.getPartialKeyLengthHeader());
    }
}

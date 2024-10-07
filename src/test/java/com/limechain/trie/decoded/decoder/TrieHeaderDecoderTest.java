package com.limechain.trie.decoded.decoder;

import com.limechain.trie.decoded.NodeVariant;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TrieHeaderDecoderTest {
    @Test
    void decodeKeyTest() throws IOException {
        byte[] expectedPartialKey = new byte[]{9};
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ScaleCodecWriter writer = new ScaleCodecWriter(out);
        writer.writeByteArray(expectedPartialKey);

        byte[] data = out.toByteArray();

        ScaleCodecReader reader = new ScaleCodecReader(data);
        byte[] partialKey = TrieKeyDecoder.decodeKey(reader, 1);
        assertArrayEquals(expectedPartialKey, partialKey);
    }

    @Test
    void decodeHeaderByteTest() throws IOException {
        int expectedPartialKeyLength = 1;
        byte[] expectedPartialKey = new byte[]{9};
        byte[] expectedStorageValue = new byte[]{1, 2, 3};
        int nodeType = NodeVariant.LEAF.bits | 1;

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ScaleCodecWriter writer = new ScaleCodecWriter(out);
        writer.writeByte(nodeType);
        writer.writeAsList(expectedPartialKey);
        writer.writeAsList(expectedStorageValue);

        byte[] data = out.toByteArray();
        ScaleCodecReader reader = new ScaleCodecReader(data);
        TrieHeaderDecoderResult headerResult = TrieHeaderDecoder.decodeHeaderByte(reader.readByte());

        assertEquals(expectedPartialKeyLength, headerResult.partialKeyLengthHeader());
    }
}

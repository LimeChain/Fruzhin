package com.limechain.internal;

import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

public class TreeDecoderTest {

    @Test
    public void decodeLeaf() throws IOException {
        byte[] expectedPartialKey = new byte[]{9};
        byte[] expectedStorageValue = new byte[]{1, 2, 3};
        byte nodeType = (byte) (DecodeHeaderResult.variantsOrderedByBitMask[0][0] | 1);
        OutputStream out = new ByteArrayOutputStream();
        ScaleCodecWriter writer = new ScaleCodecWriter(out);
        writer.writeByte(nodeType);
        writer.writeAsList(expectedPartialKey);
        writer.writeAsList(expectedStorageValue);

        byte[] data = ((ByteArrayOutputStream) out).toByteArray();

        ScaleCodecReader reader = new ScaleCodecReader(data);
        Node node = TreeDecoder.decode(reader);
        assertArrayEquals(expectedPartialKey, node.getPartialKey());
        assertArrayEquals(expectedStorageValue, node.getStorageValue());
    }
    
    @Test
    public void decodeKey() throws IOException {
        byte[] expectedPartialKey = new byte[]{9};
        OutputStream out = new ByteArrayOutputStream();
        ScaleCodecWriter writer = new ScaleCodecWriter(out);
        writer.writeAsList(expectedPartialKey);

        byte[] data = ((ByteArrayOutputStream) out).toByteArray();

        ScaleCodecReader reader = new ScaleCodecReader(data);
        byte[] partialKey = DecodeLeaf.decodeKey(reader, 1);
        assertArrayEquals(expectedPartialKey, partialKey);
    }
}

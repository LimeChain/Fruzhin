package com.limechain.internal;

import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class TreeDecoderTest {
    @Test
    public void decodeKeyTest() throws IOException {
        byte[] expectedPartialKey = new byte[]{9};
        OutputStream out = new ByteArrayOutputStream();
        ScaleCodecWriter writer = new ScaleCodecWriter(out);
        writer.writeAsList(expectedPartialKey);

        byte[] data = ((ByteArrayOutputStream) out).toByteArray();

        ScaleCodecReader reader = new ScaleCodecReader(data);
        byte[] partialKey = DecodeLeaf.decodeKey(reader, 1);
        assertArrayEquals(expectedPartialKey, partialKey);
    }

    @Test
    public void decodeHeaderByteTest() throws IOException {
        int expectedPartialKeyLength = 1;
        byte[] expectedPartialKey = new byte[]{9};
        byte[] expectedStorageValue = new byte[]{1, 2, 3};
        int nodeType = DecodeHeaderResult.variantsOrderedByBitMask[0][0] | 1;
        OutputStream out = new ByteArrayOutputStream();
        ScaleCodecWriter writer = new ScaleCodecWriter(out);
        writer.writeByte(nodeType);
        writer.writeAsList(expectedPartialKey);
        writer.writeAsList(expectedStorageValue);

        byte[] data = ((ByteArrayOutputStream) out).toByteArray();

        ScaleCodecReader reader = new ScaleCodecReader(data);
        DecodeHeaderResult headerResult = DecodeHeaderResult.decodeHeaderByte(reader.readByte());
        assertEquals(expectedPartialKeyLength, headerResult.getPartialKeyLengthHeader());
    }


    @Test
    public void decodeLeafTest() throws IOException {
        byte[] expectedPartialKey = new byte[]{9};
        byte[] expectedStorageValue = new byte[]{1, 2, 3};
        byte leafVariant = (byte) (DecodeHeaderResult.variantsOrderedByBitMask[0][0] | 1);
        OutputStream out = new ByteArrayOutputStream();
        ScaleCodecWriter writer = new ScaleCodecWriter(out);
        writer.writeByte(leafVariant);
        writer.writeAsList(expectedPartialKey);
        writer.writeAsList(expectedStorageValue);

        byte[] data = ((ByteArrayOutputStream) out).toByteArray();

        ScaleCodecReader reader = new ScaleCodecReader(data);
        Node node = TreeDecoder.decode(reader);
        assertArrayEquals(expectedPartialKey, node.getPartialKey());
        assertArrayEquals(expectedStorageValue, node.getStorageValue());
    }

    @Test
    public void decodeBranchTest() throws IOException {
        byte[] expectedPartialKey = new byte[]{9};
        byte[] childrenBitmap = new byte[]{0, 4};
        byte[] childHash = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
                11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23,
                24, 25, 26, 27, 28, 29, 30, 31, 32};
        byte branchVariant = (byte) (DecodeHeaderResult.variantsOrderedByBitMask[1][0] | 1);
        OutputStream out = new ByteArrayOutputStream();
        ScaleCodecWriter writer = new ScaleCodecWriter(out);
        writer.writeByte(branchVariant);
        writer.writeAsList(expectedPartialKey);
        writer.writeByteArray(childrenBitmap);
        writer.writeAsList(childHash);

        byte[] data = ((ByteArrayOutputStream) out).toByteArray();

        ScaleCodecReader reader = new ScaleCodecReader(data);
        Node node = TreeDecoder.decode(reader);
        assertEquals(node.getKind(), NodeKind.Branch);
        assertArrayEquals(expectedPartialKey, node.getPartialKey());
        assertNull(node.getStorageValue());
        assertArrayEquals(Node.getMerkleValueRoot(childHash), node.getChildren()[10].getMerkleValue());
    }

    @Test
    public void decodeBranchWithValueTest() throws IOException {
        byte[] expectedPartialKey = new byte[]{9};
        byte[] childrenBitmap = new byte[]{0, 4};
        byte[] expectedStorageValue = new byte[]{7, 8, 9};
        byte[] childHash = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
                11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23,
                24, 25, 26, 27, 28, 29, 30, 31, 32};
        byte branchWithValueNodeVariant = (byte) (DecodeHeaderResult.variantsOrderedByBitMask[2][0] | 1);
        OutputStream out = new ByteArrayOutputStream();
        ScaleCodecWriter writer = new ScaleCodecWriter(out);
        writer.writeByte(branchWithValueNodeVariant);
        writer.writeAsList(expectedPartialKey);
        writer.writeByteArray(childrenBitmap);
        writer.writeAsList(expectedStorageValue);
        writer.writeAsList(childHash);

        byte[] data = ((ByteArrayOutputStream) out).toByteArray();

        ScaleCodecReader reader = new ScaleCodecReader(data);
        Node node = TreeDecoder.decode(reader);
        assertEquals(node.getKind(), NodeKind.Branch);
        assertArrayEquals(expectedPartialKey, node.getPartialKey());
        assertArrayEquals(expectedStorageValue, node.getStorageValue());
        assertArrayEquals(Node.getMerkleValueRoot(childHash), node.getChildren()[10].getMerkleValue());
    }


}

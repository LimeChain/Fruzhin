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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TreeDecoderTest {

    @Test
    public void headerDecodingExceptionTest() {
        Exception e = assertThrows(TrieDecoderException.class, () -> {
            byte[] data = new byte[]{};
            ScaleCodecReader reader = new ScaleCodecReader(data);
            TreeDecoder.decode(reader);
        });
        assertTrue(e.getMessage().contains("Could not decode header"));
    }

    @Test
    public void variantExceptionTest() {
        Exception e = assertThrows(TrieDecoderException.class, () -> {
            byte[] data = new byte[]{0};
            ScaleCodecReader reader = new ScaleCodecReader(data);
            TreeDecoder.decode(reader);
        });
        assertTrue(e.getMessage().contains("Node variant is unknown"));
    }

    @Test
    public void decodeLeafExceptionTest() {
        Exception e = assertThrows(TrieDecoderException.class, () -> {
            byte[] data = new byte[]{(byte) ((byte) Variant.LEAF.bits | 63)};
            ScaleCodecReader reader = new ScaleCodecReader(data);
            TreeDecoder.decode(reader);
        });
        assertTrue(e.getMessage().contains("Could not decode header"));
    }

    @Test
    public void decodeLeafTest() throws IOException, TrieDecoderException {
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
    public void decodeBranchExceptionTest() {
        Exception e = assertThrows(TrieDecoderException.class, () -> {
            byte[] data = new byte[]{(byte) (Variant.BRANCH.bits | 63)};
            ScaleCodecReader reader = new ScaleCodecReader(data);
            TreeDecoder.decode(reader);
        });
        assertTrue(e.getMessage().contains("Could not decode header"));
    }

    @Test
    public void decodeBranchWithNoChildrenTest() throws TrieDecoderException {
        byte[] expectedPartialKey = new byte[]{9};

        byte[] data = new byte[]{(byte) (Variant.BRANCH.bits | 1), 4, 9, 0, 0};
        System.out.println(Variant.BRANCH.bits | 1);
        ScaleCodecReader reader = new ScaleCodecReader(data);
        Node node = TreeDecoder.decode(reader);
        assertEquals(node.getKind(), NodeKind.Branch);
        assertArrayEquals(expectedPartialKey, node.getPartialKey());
        assertNull(node.getStorageValue());
    }

    @Test
    public void decodeEmptyBranchExceptionTest() {
        Exception e = assertThrows(TrieDecoderException.class, () -> {
            byte[] data = new byte[]{};
            ScaleCodecReader reader = new ScaleCodecReader(data);
            TreeDecoder.decodeBranch(reader, (byte) Variant.BRANCH.bits, 1);
        });
        assertTrue(e.getMessage().contains("Could not decode partial key"));
    }

    @Test
    public void decodeBranchChildrenBitmapExceptionTest() {
        Exception e = assertThrows(TrieDecoderException.class, () -> {
            byte[] data = new byte[]{4, 9};
            ScaleCodecReader reader = new ScaleCodecReader(data);
            TreeDecoder.decodeBranch(reader, (byte) Variant.BRANCH.bits, 1);
        });
        assertTrue(e.getMessage().contains("Could not decode children bitmap"));
    }

    @Test
    public void decodeBranchChildrenExceptionTest() {
        Exception e = assertThrows(TrieDecoderException.class, () -> {
            byte[] data = new byte[]{4, 9, 0, 4};
            ScaleCodecReader reader = new ScaleCodecReader(data);
            TreeDecoder.decodeBranch(reader, (byte) Variant.BRANCH.bits, 1);
        });
        assertTrue(e.getMessage().contains("Could not decode child hash"));
    }

    @Test
    public void decodeKeyTest() throws TrieDecoderException, IOException {
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
    public void decodeHeaderByteTest() throws IOException, TrieDecoderException {
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
    public void decodeBranchWithValueTest() throws IOException, TrieDecoderException {
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

    @Test
    public void storageReadErrorTest() {
        Exception e = assertThrows(TrieDecoderException.class, () -> {
            byte[] data = new byte[]{4, 9, 0, 4};
            ScaleCodecReader reader = new ScaleCodecReader(data);
            TreeDecoder.decodeBranch(reader, (byte) DecodeHeaderResult.variantsOrderedByBitMask[2][0], 1);
        });
        assertTrue(e.getMessage().contains("Could not decode storage value"));
    }

    @Test
    public void emptyInlineNodeReadErrorTest() {
        Exception e = assertThrows(TrieDecoderException.class, () -> {
            byte[] data = new byte[]{127, 4, 9, 0b0000_0001, 0b0000_0000, 4, 1, 4, 0};
            ScaleCodecReader reader = new ScaleCodecReader(data);
            TreeDecoder.decode(reader);
        });
        return;
    }

    @Test
    public void decodeBranchTest1() throws IOException, TrieDecoderException {
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

}

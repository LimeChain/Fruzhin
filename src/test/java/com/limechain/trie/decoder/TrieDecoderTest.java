package com.limechain.trie.decoder;

import com.limechain.trie.Node;
import com.limechain.trie.NodeKind;
import com.limechain.trie.NodeVariant;
import com.limechain.trie.Trie;
import com.limechain.trie.encoder.TrieEncoder;
import com.limechain.utils.HashUtils;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrieDecoderTest {

    private final byte leaf = (byte) (NodeVariant.LEAF.bits | 1);
    private final byte hashedLeaf = (byte) (NodeVariant.LEAF_WITH_HASHED_VALUE.bits | 1);
    private final byte branch = (byte) (NodeVariant.BRANCH.bits | 1);
    private final byte[] hashedValue = HashUtils.hashWithBlake2b("hashedValue".getBytes());
    private final byte hashedBranch = (byte) (NodeVariant.BRANCH_WITH_HASHED_VALUE.bits | 1);
    private final byte branchWithValue = (byte) (NodeVariant.BRANCH_WITH_VALUE.bits | 1);

    @Test
    void headerDecodingExceptionTest() {
        Exception e = assertThrows(TrieDecoderException.class, () -> {
            byte[] data = new byte[]{};
            TrieDecoder.decode(data);
        });
        assertTrue(e.getMessage().contains("Could not decode header"));
    }

    @Test
    void unknownNodeHeaderDecodeExceptionTest() {
        Exception e = assertThrows(TrieDecoderException.class, () -> {
            byte[] data = new byte[]{0b0000_1000};
            TrieDecoder.decode(data);
        });
        assertEquals("Node header is unknown: 00001000", e.getMessage());
    }

    @Test
    void decodeLeafExceptionTest() {
        Exception e = assertThrows(TrieDecoderException.class, () -> {
            byte[] data = new byte[]{(byte) ((byte) NodeVariant.LEAF.bits | 63)};
            TrieDecoder.decode(data);
        });
        assertTrue(e.getMessage().contains("Could not decode header"));
    }

    @Test
    void decodeLeafTest() throws IOException {
        byte[] expectedPartialKey = new byte[]{9};
        byte[] expectedStorageValue = new byte[]{1, 2, 3};

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ScaleCodecWriter writer = new ScaleCodecWriter(out);
        writer.writeByte(leaf);
        writer.writeByteArray(expectedPartialKey);
        writer.writeAsList(expectedStorageValue);

        byte[] data = out.toByteArray();
        Node node = TrieDecoder.decode(data);

        assertArrayEquals(expectedPartialKey, node.getPartialKey());
        assertArrayEquals(expectedStorageValue, node.getStorageValue());
    }

    @Test
    void decodeBranchExceptionTest() {
        Exception e = assertThrows(TrieDecoderException.class, () -> {
            byte[] data = new byte[]{(byte) (NodeVariant.BRANCH.bits | 63)};
            TrieDecoder.decode(data);
        });
        assertTrue(e.getMessage().contains("Could not decode header"));
    }

    @Test
    void decodeBranchWithNoChildrenTest() throws IOException {
        byte[] expectedPartialKey = new byte[]{9};
        byte[] childrenBitmap = new byte[]{0, 0};

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ScaleCodecWriter writer = new ScaleCodecWriter(out);
        writer.writeByte(branch);
        writer.writeByteArray(expectedPartialKey);
        writer.writeByteArray(childrenBitmap);

        Node node = TrieDecoder.decode(out.toByteArray());

        Assertions.assertEquals(NodeKind.Branch, node.getKind());
        assertArrayEquals(expectedPartialKey, node.getPartialKey());
        assertNull(node.getStorageValue());
    }

    @Test
    void decodeBranchWithValueTest() throws IOException {
        byte[] expectedPartialKey = new byte[]{9};
        byte[] childrenBitmap = new byte[]{0, 4};
        byte[] expectedStorageValue = new byte[]{7, 8, 9};
        byte[] childHash = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
                11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23,
                24, 25, 26, 27, 28, 29, 30, 31, 32};

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ScaleCodecWriter writer = new ScaleCodecWriter(out);
        writer.writeByte(branchWithValue);
        writer.writeByteArray(expectedPartialKey);
        writer.writeByteArray(childrenBitmap);
        writer.writeAsList(expectedStorageValue);
        writer.writeAsList(childHash);

        byte[] data = out.toByteArray();
        Node node = TrieDecoder.decode(data);

        assertEquals(NodeKind.Branch, node.getKind());
        assertArrayEquals(expectedPartialKey, node.getPartialKey());
        assertArrayEquals(expectedStorageValue, node.getStorageValue());
        assertArrayEquals(Trie.getMerkleValueRoot(childHash),
                Trie.getMerkleValueRoot(node.getChildren()[10].getMerkleValue()));
    }

    @Test
    void decodeBranchWithInlinedBranchAndLeafTest() {
        var node = new Node() {{
            this.setPartialKey(new byte[]{1});
            this.setStorageValue(new byte[]{1});
            this.setChildrenAt(new Node() {{
                this.setPartialKey(new byte[]{2});
                this.setStorageValue(new byte[]{2});
            }}, 0);
            this.setChildrenAt(new Node() {{
                this.setPartialKey(new byte[]{3});
                this.setStorageValue(new byte[]{3});
                this.setChildrenAt(new Node() {{
                    this.setPartialKey(new byte[]{4});
                    this.setStorageValue(new byte[]{4});
                }}, 0);

            }}, 1);
        }};
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        TrieEncoder.encode(node, buffer);

        Node decodedNode = TrieDecoder.decode(buffer.toByteArray());
        assertEquals(NodeKind.Branch, decodedNode.getKind());
        assertArrayEquals(new byte[]{1}, decodedNode.getPartialKey());
        assertArrayEquals(new byte[]{1}, decodedNode.getStorageValue());
        assertArrayEquals(new byte[]{2}, decodedNode.getChildren()[0].getPartialKey());
        assertArrayEquals(new byte[]{2}, decodedNode.getChildren()[0].getStorageValue());
        assertArrayEquals(new byte[]{3}, decodedNode.getChildren()[1].getPartialKey());
        assertArrayEquals(new byte[]{3}, decodedNode.getChildren()[1].getStorageValue());
        assertArrayEquals(new byte[]{4}, decodedNode.getChildren()[1].getChildren()[0].getPartialKey());
        assertArrayEquals(new byte[]{4}, decodedNode.getChildren()[1].getChildren()[0].getStorageValue());
    }

    @Test
    void decodeLeafWithHashedValue() throws IOException {
        byte[] expectedPartialKey = new byte[]{9};

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ScaleCodecWriter writer = new ScaleCodecWriter(out);
        writer.writeByte(hashedLeaf);
        writer.writeByteArray(expectedPartialKey);
        writer.writeByteArray(hashedValue);

        Node decodedNode = TrieDecoder.decode(out.toByteArray());
        assertEquals(NodeKind.Leaf, decodedNode.getKind());
        assertArrayEquals(expectedPartialKey, decodedNode.getPartialKey());
        assertArrayEquals(hashedValue, decodedNode.getStorageValue());
        assertTrue(decodedNode.isValueHashed());
    }

    @Test
    void decodeLeafWithTooShortHashedValueFails() throws IOException {
        byte[] expectedPartialKey = new byte[]{9};
        byte[] tooShortHashedStorageValue = new byte[]{0b0000_0000};

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ScaleCodecWriter writer = new ScaleCodecWriter(out);
        writer.writeByte(hashedLeaf);
        writer.writeByteArray(expectedPartialKey);
        writer.writeAsList(tooShortHashedStorageValue);
        byte[] encodedBytes = out.toByteArray();

        Exception e = assertThrows(TrieDecoderException.class, () -> {
            TrieDecoder.decode(encodedBytes);
        });
        assertTrue(e.getMessage().contains("Could not decode hashed storage value"));
    }

    @Test
    void decodeBranchWithHashedValue() throws IOException {
        byte[] expectedPartialKey = new byte[]{9};
        byte[] childrenBitmap = new byte[]{0b0000_0000, 0b0000_0000};

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ScaleCodecWriter writer = new ScaleCodecWriter(out);
        writer.writeByte(hashedBranch);
        writer.writeByteArray(expectedPartialKey);
        writer.writeByteArray(childrenBitmap);
        writer.writeByteArray(hashedValue);

        Node decodedNode = TrieDecoder.decode(out.toByteArray());
        assertEquals(NodeKind.Branch, decodedNode.getKind());
        assertArrayEquals(expectedPartialKey, decodedNode.getPartialKey());
        assertArrayEquals(hashedValue, decodedNode.getStorageValue());
        assertTrue(decodedNode.isValueHashed());
    }

    @Test
    void decodeBranchWithTooShortHashedValueFails() throws IOException {
        byte[] partialKey = new byte[]{9};
        byte[] tooShortHashedStorageValue = new byte[]{0b0000_0000};
        byte[] childrenBitmap = new byte[]{0b0000_0000, 0b0000_0000};

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ScaleCodecWriter writer = new ScaleCodecWriter(out);
        writer.writeByte(hashedBranch);
        writer.writeByteArray(partialKey);
        writer.writeByteArray(childrenBitmap);
        writer.writeAsList(tooShortHashedStorageValue);
        byte[] encodedBytes = out.toByteArray();

        Exception e = assertThrows(TrieDecoderException.class,
                () -> TrieDecoder.decode(encodedBytes)
        );
        assertTrue(e.getMessage().contains("Could not decode hashed storage value"));
    }

}

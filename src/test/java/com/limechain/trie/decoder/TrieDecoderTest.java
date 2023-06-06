package com.limechain.trie.decoder;

import com.limechain.trie.Node;
import com.limechain.trie.NodeKind;
import com.limechain.trie.NodeVariant;
import com.limechain.trie.Trie;
import com.limechain.trie.еncoder.TrieEncoder;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TrieDecoderTest {
    @Test
    public void headerDecodingExceptionTest() {
        Exception e = assertThrows(TrieDecoderException.class, () -> {
            byte[] data = new byte[]{};
            TrieDecoder.decode(data);
        });
        assertTrue(e.getMessage().contains("Could not decode header"));
    }

    @Test
    public void variantExceptionTest() {
        Exception e = assertThrows(TrieDecoderException.class, () -> {
            byte[] data = new byte[]{0};
            TrieDecoder.decode(data);
        });
        assertTrue(e.getMessage().contains("Node variant is unknown"));
    }

    @Test
    public void decodeLeafExceptionTest() {
        Exception e = assertThrows(TrieDecoderException.class, () -> {
            byte[] data = new byte[]{(byte) ((byte) NodeVariant.LEAF.bits | 63)};
            TrieDecoder.decode(data);
        });
        assertTrue(e.getMessage().contains("Could not decode header"));
    }

    @Test
    public void decodeLeafTest() throws IOException, TrieDecoderException {
        byte[] expectedPartialKey = new byte[]{9};
        byte[] expectedStorageValue = new byte[]{1, 2, 3};
        byte leafVariant = (byte) (NodeVariant.LEAF.bits | 1);

        OutputStream out = new ByteArrayOutputStream();
        ScaleCodecWriter writer = new ScaleCodecWriter(out);
        writer.writeByte(leafVariant);
        writer.writeByteArray(expectedPartialKey);
        writer.writeAsList(expectedStorageValue);

        byte[] data = ((ByteArrayOutputStream) out).toByteArray();
        Node node = TrieDecoder.decode(data);

        assertArrayEquals(expectedPartialKey, node.getPartialKey());
        assertArrayEquals(expectedStorageValue, node.getStorageValue());
    }

    @Test
    public void decodeBranchExceptionTest() {
        Exception e = assertThrows(TrieDecoderException.class, () -> {
            byte[] data = new byte[]{(byte) (NodeVariant.BRANCH.bits | 63)};
            TrieDecoder.decode(data);
        });
        assertTrue(e.getMessage().contains("Could not decode header"));
    }

    @Test
    public void decodeBranchWithNoChildrenTest() throws TrieDecoderException {
        byte[] expectedPartialKey = new byte[]{9};

        byte[] data = new byte[]{(byte) (NodeVariant.BRANCH.bits | 1), 9, 0, 0};

        Node node = TrieDecoder.decode(data);

        Assertions.assertEquals(NodeKind.Branch, node.getKind());
        assertArrayEquals(expectedPartialKey, node.getPartialKey());
        assertNull(node.getStorageValue());
    }

    @Test
    public void decodeBranchWithValueTest() throws IOException, TrieDecoderException {
        byte[] expectedPartialKey = new byte[]{9};
        byte[] childrenBitmap = new byte[]{0, 4};
        byte[] expectedStorageValue = new byte[]{7, 8, 9};
        byte[] childHash = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10,
                11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23,
                24, 25, 26, 27, 28, 29, 30, 31, 32};
        byte branchWithValueNodeVariant = (byte) (NodeVariant.BRANCH_WITH_VALUE.bits | 1);

        OutputStream out = new ByteArrayOutputStream();
        ScaleCodecWriter writer = new ScaleCodecWriter(out);
        writer.writeByte(branchWithValueNodeVariant);
        writer.writeByteArray(expectedPartialKey);
        writer.writeByteArray(childrenBitmap);
        writer.writeAsList(expectedStorageValue);
        writer.writeAsList(childHash);

        byte[] data = ((ByteArrayOutputStream) out).toByteArray();
        Node node = TrieDecoder.decode(data);

        assertEquals(node.getKind(), NodeKind.Branch);
        assertArrayEquals(expectedPartialKey, node.getPartialKey());
        assertArrayEquals(expectedStorageValue, node.getStorageValue());
        assertArrayEquals(Trie.getMerkleValueRoot(childHash), Trie.getMerkleValueRoot(node.getChildren()[10].getMerkleValue()));
    }

    @Test
    public void decodeBranchWithInlinedBranchAndLeafTest() throws Exception {
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
        assertEquals(decodedNode.getKind(), NodeKind.Branch);
        assertArrayEquals(new byte[]{1}, decodedNode.getPartialKey());
        assertArrayEquals(new byte[]{1}, decodedNode.getStorageValue());
        assertArrayEquals(new byte[]{2}, decodedNode.getChildren()[0].getPartialKey());
        assertArrayEquals(new byte[]{2}, decodedNode.getChildren()[0].getStorageValue());
        assertArrayEquals(new byte[]{3}, decodedNode.getChildren()[1].getPartialKey());
        assertArrayEquals(new byte[]{3}, decodedNode.getChildren()[1].getStorageValue());
        assertArrayEquals(new byte[]{4}, decodedNode.getChildren()[1].getChildren()[0].getPartialKey());
        assertArrayEquals(new byte[]{4}, decodedNode.getChildren()[1].getChildren()[0].getStorageValue());
    }
}

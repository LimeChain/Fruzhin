package com.limechain.internal.tree.decoder;

import com.limechain.internal.Node;
import com.limechain.internal.NodeKind;
import com.limechain.internal.TreeEncoder;
import com.limechain.internal.Variant;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
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
        byte leafVariant = (byte) (Variant.LEAF.bits | 1);

        OutputStream out = new ByteArrayOutputStream();
        ScaleCodecWriter writer = new ScaleCodecWriter(out);
        writer.writeByte(leafVariant);
        writer.writeByteArray(expectedPartialKey);
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

        byte[] data = new byte[]{(byte) (Variant.BRANCH.bits | 1), 9, 0, 0};

        ScaleCodecReader reader = new ScaleCodecReader(data);
        Node node = TreeDecoder.decode(reader);

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
        byte branchWithValueNodeVariant = (byte) (Variant.BRANCH_WITH_VALUE.bits | 1);

        OutputStream out = new ByteArrayOutputStream();
        ScaleCodecWriter writer = new ScaleCodecWriter(out);
        writer.writeByte(branchWithValueNodeVariant);
        writer.writeByteArray(expectedPartialKey);
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

    @Disabled("Children encoding is not working at the moment")
    @Test
    public void decodeBranchWithInlinedBranchAndLeafTest() throws Exception {
        var node = new Node() {{
            this.setPartialKey(new byte[]{1});
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
        TreeEncoder.encode(node, buffer);

        ScaleCodecReader reader = new ScaleCodecReader(buffer.toByteArray());
        Node decodedNode = TreeDecoder.decode(reader);
        assertEquals(decodedNode.getKind(), NodeKind.Branch);
        assertArrayEquals(new byte[]{1}, decodedNode.getPartialKey());
        assertArrayEquals(new byte[]{2}, decodedNode.getStorageValue());
    }
}

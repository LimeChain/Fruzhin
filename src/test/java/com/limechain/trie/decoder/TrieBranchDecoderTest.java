package com.limechain.trie.decoder;

import com.limechain.trie.Node;
import com.limechain.trie.NodeKind;
import com.limechain.trie.NodeVariant;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.scale.ScaleCodecWriter;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static com.limechain.trie.decoder.TrieBranchDecoder.decode;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrieBranchDecoderTest {

    @Test
    void decodeEmptyBranchExceptionTest() {
        byte[] data = new byte[]{};
        ScaleCodecReader reader = new ScaleCodecReader(data);
        Exception e = assertThrows(TrieDecoderException.class, () ->
                decode(reader, NodeVariant.BRANCH, 1));
        assertTrue(e.getMessage().contains("Could not decode partial key"));
    }

    @Test
    void decodeBranchChildrenBitmapExceptionTest() {
        byte[] data = new byte[]{9};
        ScaleCodecReader reader = new ScaleCodecReader(data);
        Exception e = assertThrows(TrieDecoderException.class, () ->
                decode(reader, NodeVariant.BRANCH, 1));
        assertTrue(e.getMessage().contains("Could not decode children bitmap"));
    }

    @Test
    void decodeBranchChildrenExceptionTest() {
        byte[] data = new byte[]{9, 0, 4};
        ScaleCodecReader reader = new ScaleCodecReader(data);
        Exception e = assertThrows(TrieDecoderException.class, () ->
                decode(reader, NodeVariant.BRANCH, 1));
        assertTrue(e.getMessage().contains("Could not decode child hash"));
    }

    @Test
    void storageReadExceptionTest() {
        byte[] data = new byte[]{9, 0, 4};
        ScaleCodecReader reader = new ScaleCodecReader(data);
        Exception e = assertThrows(TrieDecoderException.class, () ->
                decode(reader, NodeVariant.BRANCH_WITH_VALUE, 1));
        assertTrue(e.getMessage().contains("Could not decode storage value"));
    }

    @Test
    void emptyInlineNodeReadExceptionTest() {
        byte[] data = new byte[]{1, 0b0000_0001, 0b0000_0000, 4, 1, 0};
        ScaleCodecReader reader = new ScaleCodecReader(data);
        Exception e = assertThrows(TrieDecoderException.class, () ->
                decode(reader, NodeVariant.BRANCH_WITH_VALUE, 1));
        assertTrue(e.getMessage().contains("Could not decode child hash"));
    }

    @Test
    void branchWithValueDecodeSuccess() throws IOException {
        var scaleHash = new byte[32];
        for (int i = 0; i < 32; i++) {
            scaleHash[i] = (byte) i;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ScaleCodecWriter writer = new ScaleCodecWriter(out);
        // partial key
        writer.writeByteArray(new byte[]{9});
        // children bitmap
        writer.writeByteArray(new byte[]{0, 4});
        // storage value
        writer.writeAsList(new byte[]{7, 8, 9});
        // child hash
        writer.writeAsList(scaleHash);
        ScaleCodecReader reader = new ScaleCodecReader(out.toByteArray());
        Node node = TrieBranchDecoder.decode(reader, NodeVariant.BRANCH_WITH_VALUE, 1);

        assertEquals(NodeKind.Branch, node.getKind());
        assertArrayEquals(new byte[]{9}, node.getPartialKey());
        assertArrayEquals(new byte[]{7, 8, 9}, node.getStorageValue());
        assertEquals(1, node.getDescendants());
        assertArrayEquals(scaleHash, node.getChild(10).getMerkleValue());
    }

    @Test
    void branchWithInlinedBranchAndLeafReadSuccess() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ScaleCodecWriter writer = new ScaleCodecWriter(out);

        // key data
        writer.writeByteArray(new byte[]{1});
        // children bitmap
        writer.writeByteArray(new byte[]{0b0000_0011, 0b0000_0000});
        // top level inlined leaf
        ByteArrayOutputStream inlineLeaf = encodeInlinedLeaf(
                (byte) (NodeVariant.LEAF.bits | 1),
                (byte) 2,
                new byte[]{2}
        );
        writer.writeAsList(inlineLeaf.toByteArray());
        // inlined leaf for the inlined branch
        byte[] inlinedLeaf2 = encodeInlinedLeaf(
                (byte) (NodeVariant.LEAF.bits | 1),
                (byte) 4,
                new byte[]{4}
        ).toByteArray();

        // top level inlined branch
        ByteArrayOutputStream inlinedBranch = encodeInlinedBranch(
                (byte) (NodeVariant.BRANCH_WITH_VALUE.bits | 1),
                (byte) 3,
                new byte[]{0b0000_0001, 0b0000_0000},
                new byte[]{3},
                inlinedLeaf2
        );
        writer.writeAsList(inlinedBranch.toByteArray());
        ScaleCodecReader reader = new ScaleCodecReader(out.toByteArray());
        Node node = TrieBranchDecoder.decode(reader, NodeVariant.BRANCH, 1);

        assertEquals(NodeKind.Branch, node.getKind());
        assertArrayEquals(new byte[]{1}, node.getPartialKey());
        assertNull(node.getStorageValue());
        assertEquals(3, node.getDescendants());

        Node inlinedLeaf = node.getChild(0);
        assertEquals(NodeKind.Leaf, inlinedLeaf.getKind());
        assertArrayEquals(new byte[]{2}, inlinedLeaf.getStorageValue());
        assertArrayEquals(new byte[]{2}, inlinedLeaf.getPartialKey());

        Node inlinedNodeBranch = node.getChild(1);
        assertEquals(NodeKind.Branch, inlinedNodeBranch.getKind());
        assertArrayEquals(new byte[]{3}, inlinedNodeBranch.getPartialKey());
        assertArrayEquals(new byte[]{3}, inlinedNodeBranch.getPartialKey());
        assertEquals(1, inlinedNodeBranch.getDescendants());

        Node inlinedBranchChild = inlinedNodeBranch.getChild(0);
        assertEquals(NodeKind.Leaf, inlinedBranchChild.getKind());
        assertArrayEquals(new byte[]{4}, inlinedBranchChild.getStorageValue());
        assertArrayEquals(new byte[]{4}, inlinedBranchChild.getPartialKey());
    }

    private ByteArrayOutputStream encodeInlinedBranch(byte partialKey, byte key, byte[] childrenBitmap,
                                                      byte[] storageValue, byte[] children) throws IOException {
        ByteArrayOutputStream inlineBranchWriter = new ByteArrayOutputStream();
        inlineBranchWriter.write(new byte[]{partialKey, key});
        inlineBranchWriter.write(childrenBitmap);
        ScaleCodecWriter leafWriter = new ScaleCodecWriter(inlineBranchWriter);
        leafWriter.writeAsList(storageValue);
        leafWriter.writeAsList(children);
        return inlineBranchWriter;

    }

    private ByteArrayOutputStream encodeInlinedLeaf(byte partialKey, byte key, byte[] storageValue) throws IOException {
        ByteArrayOutputStream inlineLeafWriter = new ByteArrayOutputStream();
        ScaleCodecWriter leafWriter = new ScaleCodecWriter(inlineLeafWriter);
        leafWriter.writeByteArray(new byte[]{partialKey, key});
        leafWriter.writeAsList(storageValue);
        return inlineLeafWriter;
    }

}

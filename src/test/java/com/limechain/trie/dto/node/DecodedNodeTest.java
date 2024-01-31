package com.limechain.trie.dto.node;

import com.limechain.trie.dto.node.exceptions.NodeEncodingException;
import com.limechain.trie.structure.nibble.Nibbles;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DecodedNodeTest {

    @Test
    void decodeTest() {
        byte[] expectedEncoded = new byte[]{
                (byte) 194, 99, (byte) 192, 0, 0, (byte) 128, (byte) 129, (byte) 254, 111, 21, 39, (byte) 188,
                (byte) 215, 18, (byte) 139, 76, (byte) 128, (byte) 157, 108, 33, (byte) 139, (byte) 232, 34, 73,
                0, 21, (byte) 202, 54, 18, 71, (byte) 145, 117, 47, (byte) 222, (byte) 189, 93, 119, 68, (byte) 128,
                108, (byte) 211, 105, 98, 122, (byte) 206, (byte) 246, 73, 77, (byte) 237, 51, 77, 26, (byte) 166, 1,
                52, (byte) 179, (byte) 173, 43, 89, (byte) 219, 104, (byte) 196, (byte) 190, (byte) 208, (byte) 128,
                (byte) 135, (byte) 177, 13, (byte) 185, 111, (byte) 175
        };

        byte[] expectedChild1 = new byte[]{(byte) 129, (byte) 254, 111, 21, 39, (byte) 188, (byte) 215, 18, (byte) 139,
                76, (byte) 128, (byte) 157, 108, 33, (byte) 139, (byte) 232, 34, 73, 0, 21, (byte) 202, 54, 18, 71,
                (byte) 145, 117, 47, (byte) 222, (byte) 189, 93, 119, 68};

        byte[] expectedChild2 = new byte[]{108, (byte) 211, 105, 98, 122, (byte) 206, (byte) 246, 73, 77, (byte) 237,
                51, 77, 26, (byte) 166, 1, 52, (byte) 179, (byte) 173, 43, 89, (byte) 219, 104, (byte) 196, (byte) 190,
                (byte) 208, (byte) 128, (byte) 135, (byte) 177, 13, (byte) 185, 111, (byte) 175};

        List<Byte> boxedExpectedEncoded = Arrays.asList(ArrayUtils.toObject(expectedEncoded));
        List<Byte> boxedExpectedChild1 = Arrays.asList(ArrayUtils.toObject(expectedChild1));
        List<Byte> boxedExpectedChild2 = Arrays.asList(ArrayUtils.toObject(expectedChild2));

        StorageValue expectedStorageValue = new StorageValue(new byte[]{}, false);
        Nibbles expectedPartialKey = Nibbles.fromHexString("63");

        DecodedNode<List<Byte>> decoded = DecodedNode.decode(expectedEncoded);
        assertNotNull(decoded);

        assertEquals(boxedExpectedEncoded, decoded.encode());
        assertEquals(expectedPartialKey, decoded.getPartialKey());
        assertEquals(expectedStorageValue, decoded.getStorageValue());

        List<List<Byte>> children = decoded.getChildren();

        long notNullChildrenCount = children.stream().filter(Objects::nonNull).count();
        assertEquals(2, notNullChildrenCount);

        assertEquals(boxedExpectedChild1, children.get(6));
        assertEquals(boxedExpectedChild2, children.get(7));
    }

    @Test
    void emptyNodeEncodesSuccessfully() {
        var decodedNode = new DecodedNode<>(
            List.of(),
            Nibbles.EMPTY,
            null
        );
        assertDoesNotThrow(decodedNode::encode);
    }

    @Test
    void nodeOnlyWithPartialKeyThrowsException() {
        var decodedNode = new DecodedNode<>(
                List.of(),
                Nibbles.fromHexString("2"),
                null
        );
        assertThrows(NodeEncodingException.class, decodedNode::encode);
    }

    @Test
    void exactMultiplePKLenInHeaderAddsZeroByteWhenEncoding() {
        DecodedNode<List<Byte>> decoded = new DecodedNode<>(
            // a branch node
            Nibbles.ALL.stream().map(__ -> (List<Byte>)null).toList(),
            // with exactly 63 = 2^6 - 1 nibbles (6 bits allocated for the pklen due to the node variant)
            Nibbles.fromHexString("77e6857fb1d0e409376122fee3ad4f84e7b9012096b41c4eb3aaf947f6ea429"),
            new StorageValue(new byte[]{0, 0}, false)
        );

        // Referring to this corner case:
        // https://github.com/smol-dot/smoldot/blob/21be5a1abaebeaf7270a744485b4551da8636fb1/lib/src/trie/trie_node.rs#L76-L80
        assertEquals((byte) 0, decoded.encode().get(1),
            "Zero byte must be added to indicate end of pk len in header.");
    }
}
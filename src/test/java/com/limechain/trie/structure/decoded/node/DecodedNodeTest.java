package com.limechain.trie.structure.decoded.node;

import com.limechain.trie.structure.decoded.node.exceptions.NodeEncodingException;
import com.limechain.trie.structure.nibble.Nibble;
import com.limechain.trie.structure.nibble.Nibbles;
import org.apache.commons.lang3.ArrayUtils;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collection;
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
        int expectedNotNullChildrenCount = 2;
        Nibbles expectedPartialKey = Nibbles.fromHexString("63");

        DecodedNode<Nibbles, List<Byte>> decoded = DecodedNode.decode(expectedEncoded);
        assertNotNull(decoded);

        List<Byte> actualEncoded = decoded.encode().flatMap(Collection::stream).toList();
        assertEquals(boxedExpectedEncoded, actualEncoded);

        assertEquals(expectedPartialKey, decoded.getPartialKey());

        StorageValue storageValue = decoded.getStorageValue();
        assertEquals(expectedStorageValue, storageValue);

        List<List<Byte>> children = decoded.getChildren();
        int notNullChildrenCount = children.stream().filter(Objects::nonNull).toList().size();
        assertEquals(expectedNotNullChildrenCount, notNullChildrenCount);

        assertEquals(boxedExpectedChild1, children.get(6));
        assertEquals(boxedExpectedChild2, children.get(7));
    }

    @Test
    void emptyNodeEncodesSuccessfully() {
        var decodedNode = new DecodedNode<>(
                List.of(),
                List.of(),
                null
        );
        assertDoesNotThrow(decodedNode::encode);
    }

    @Test
    void nodeOnlyWithPartialKeyThrowsException() {
        var decodedNode = new DecodedNode<>(
                List.of(),
                List.of(Nibble.fromInt(2)),
                null
        );
        assertThrows(NodeEncodingException.class, decodedNode::encode);
    }
}
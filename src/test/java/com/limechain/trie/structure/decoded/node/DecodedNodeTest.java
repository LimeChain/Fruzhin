package com.limechain.trie.structure.decoded.node;

import com.limechain.trie.structure.nibble.Nibble;
import com.limechain.trie.structure.nibble.Nibbles;
import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.util.ArrayUtils;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

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

        DecodedNode<Nibbles, List<Byte>> decoded = DecodedNode.decode(expectedEncoded);
        assertNotNull(decoded);

    }

    @Test
    void extractPartialKey() {
    }

    @Test
    void extractChildrenBitmap() {
    }

    @Test
    void extractStorageValue() {
    }

    @Test
    void extractChildren() {
    }

    @Test
    void getStorageValue() {
    }
}
package com.limechain.trie.decoded.decoder;

import com.limechain.exception.trie.TrieDecoderException;
import com.limechain.trie.decoded.Node;
import com.limechain.trie.decoded.NodeVariant;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrieLeafDecoderTest {
    @Test
    void decodeKeyExceptionTest() {
        byte[] data = new byte[]{};
        ScaleCodecReader reader = new ScaleCodecReader(data);
        Exception e = assertThrows(TrieDecoderException.class, () ->
                TrieLeafDecoder.decode(reader, NodeVariant.LEAF, 1));
        assertTrue(e.getMessage().contains("Could not decode partial key"));
    }

    @Test
    void decodeBadStorageExceptionTest() {
        byte[] data = new byte[]{9, (byte) 255, (byte) 255};
        ScaleCodecReader reader = new ScaleCodecReader(data);
        Exception e = assertThrows(TrieDecoderException.class, () ->
                TrieLeafDecoder.decode(reader, NodeVariant.LEAF, 1));
        assertTrue(e.getMessage().contains("Could not decode storage value"));
    }

    @Test
    void decodeMissingStorageExceptionTest() {
        byte[] data = new byte[]{9};
        ScaleCodecReader reader = new ScaleCodecReader(data);
        Exception e = assertThrows(TrieDecoderException.class, () ->
                TrieLeafDecoder.decode(reader, NodeVariant.LEAF, 1));
        assertTrue(e.getMessage().contains("Could not decode storage value"));
    }

    @Test
    void decodeEmptyStorageTest() {
        byte[] data = new byte[]{9, 0};
        byte[] expectedStorage = new byte[]{};

        ScaleCodecReader reader = new ScaleCodecReader(data);
        Node leaf = TrieLeafDecoder.decode(reader, NodeVariant.LEAF, 1);
        assertArrayEquals(expectedStorage, leaf.getStorageValue());
    }

    @Test
    void decodeStorageTest() {
        byte[] data = new byte[]{9, 20, 1, 2, 3, 4, 5};
        byte[] expectedStorage = new byte[]{1, 2, 3, 4, 5};

        ScaleCodecReader reader = new ScaleCodecReader(data);
        Node leaf = TrieLeafDecoder.decode(reader, NodeVariant.LEAF, 1);
        assertArrayEquals(expectedStorage, leaf.getStorageValue());
    }

}

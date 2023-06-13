package com.limechain.trie.decoder;

import com.limechain.trie.Node;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TrieLeafDecoderTest {
    @Test
    public void decodeKeyExceptionTest() {
        Exception e = assertThrows(TrieDecoderException.class, () -> {
            byte[] data = new byte[]{};
            ScaleCodecReader reader = new ScaleCodecReader(data);
            TrieLeafDecoder.decode(reader, 1);
        });
        assertTrue(e.getMessage().contains("Could not decode partial key"));
    }

    @Test
    public void decodeBadStorageExceptionTest() {
        Exception e = assertThrows(TrieDecoderException.class, () -> {
            byte[] data = new byte[]{4, 9, 127, 127};
            ScaleCodecReader reader = new ScaleCodecReader(data);
            TrieLeafDecoder.decode(reader, 1);
        });
        assertTrue(e.getMessage().contains("Could not decode storage value"));
    }

    @Test
    public void decodeMissingStorageExceptionTest() {
        Exception e = assertThrows(TrieDecoderException.class, () -> {
            byte[] data = new byte[]{4, 9};
            ScaleCodecReader reader = new ScaleCodecReader(data);
            TrieLeafDecoder.decode(reader, 1);
        });
        assertTrue(e.getMessage().contains("Could not decode storage value"));
    }

    @Test
    public void decodeEmptyStorageTest() {
        byte[] data = new byte[]{9, 0};
        byte[] expectedStorage = new byte[]{};

        ScaleCodecReader reader = new ScaleCodecReader(data);
        Node leaf = TrieLeafDecoder.decode(reader, 1);
        assertArrayEquals(expectedStorage, leaf.getStorageValue());
    }

    @Test
    public void decodeStorageTest() {
        byte[] data = new byte[]{9, 20, 1, 2, 3, 4, 5};
        byte[] expectedStorage = new byte[]{1, 2, 3, 4, 5};

        ScaleCodecReader reader = new ScaleCodecReader(data);
        Node leaf = TrieLeafDecoder.decode(reader, 1);
        assertArrayEquals(expectedStorage, leaf.getStorageValue());
    }

}

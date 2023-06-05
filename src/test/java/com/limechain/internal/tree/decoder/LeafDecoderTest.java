package com.limechain.internal.tree.decoder;

import com.limechain.internal.Node;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class LeafDecoderTest {
    @Test
    public void decodeKeyExceptionTest() {
        Exception e = assertThrows(TrieDecoderException.class, () -> {
            byte[] data = new byte[]{};
            ScaleCodecReader reader = new ScaleCodecReader(data);
            LeafDecoder.decodeLeaf(reader, 1);
        });
        assertTrue(e.getMessage().contains("Could not decode partial key"));
    }

    @Test
    public void decodeBadStorageExceptionTest() {
        Exception e = assertThrows(TrieDecoderException.class, () -> {
            byte[] data = new byte[]{4, 9, 127, 127};
            ScaleCodecReader reader = new ScaleCodecReader(data);
            LeafDecoder.decodeLeaf(reader, 1);
        });
        assertTrue(e.getMessage().contains("Could not decode storage value"));
    }

    @Test
    public void decodeMissingStorageExceptionTest() {
        Exception e = assertThrows(TrieDecoderException.class, () -> {
            byte[] data = new byte[]{4, 9};
            ScaleCodecReader reader = new ScaleCodecReader(data);
            LeafDecoder.decodeLeaf(reader, 1);
        });
        assertTrue(e.getMessage().contains("Could not decode storage value"));
    }

    @Test
    public void decodeEmptyStorageTest() throws TrieDecoderException {
        byte[] data = new byte[]{9, 0};
        byte[] expectedStorage = new byte[]{};

        ScaleCodecReader reader = new ScaleCodecReader(data);
        Node leaf = LeafDecoder.decodeLeaf(reader, 1);
        assertArrayEquals(expectedStorage, leaf.getStorageValue());
    }

    @Test
    public void decodeStorageTest() throws TrieDecoderException {
        byte[] data = new byte[]{9, 20, 1, 2, 3, 4, 5};
        byte[] expectedStorage = new byte[]{1, 2, 3, 4, 5};

        ScaleCodecReader reader = new ScaleCodecReader(data);
        Node leaf = LeafDecoder.decodeLeaf(reader, 1);
        assertArrayEquals(expectedStorage, leaf.getStorageValue());
    }

}

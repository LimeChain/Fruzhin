package com.limechain.trie.decoder;

import com.limechain.trie.NodeVariant;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import org.junit.jupiter.api.Test;

import static com.limechain.trie.decoder.TrieBranchDecoder.decode;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TrieBranchDecoderTest {
    @Test
    public void decodeEmptyBranchExceptionTest() {
        Exception e = assertThrows(TrieDecoderException.class, () -> {
            byte[] data = new byte[]{};
            ScaleCodecReader reader = new ScaleCodecReader(data);
            decode(reader, (byte) NodeVariant.BRANCH.bits, 1);
        });
        assertTrue(e.getMessage().contains("Could not decode partial key"));
    }

    @Test
    public void decodeBranchChildrenBitmapExceptionTest() {
        Exception e = assertThrows(TrieDecoderException.class, () -> {
            byte[] data = new byte[]{9};
            ScaleCodecReader reader = new ScaleCodecReader(data);
            decode(reader, (byte) NodeVariant.BRANCH.bits, 1);
        });
        assertTrue(e.getMessage().contains("Could not decode children bitmap"));
    }

    @Test
    public void decodeBranchChildrenExceptionTest() {
        Exception e = assertThrows(TrieDecoderException.class, () -> {
            byte[] data = new byte[]{9, 0, 4};
            ScaleCodecReader reader = new ScaleCodecReader(data);
            decode(reader, (byte) NodeVariant.BRANCH.bits, 1);
        });
        assertTrue(e.getMessage().contains("Could not decode child hash"));
    }

    @Test
    public void storageReadExceptionTest() {
        Exception e = assertThrows(TrieDecoderException.class, () -> {
            byte[] data = new byte[]{9, 0, 4};
            ScaleCodecReader reader = new ScaleCodecReader(data);
            decode(reader, (byte) NodeVariant.BRANCH_WITH_VALUE.bits, 1);
        });
        assertTrue(e.getMessage().contains("Could not decode storage value"));
    }

    @Test
    public void emptyInlineNodeReadExceptionTest() {
        Exception e = assertThrows(TrieDecoderException.class, () -> {
            byte[] data = new byte[]{9, 0b0000_0001, 0b0000_0000, 4, 1, 4, 0};
            ScaleCodecReader reader = new ScaleCodecReader(data);
            decode(reader, (byte) NodeVariant.BRANCH_WITH_VALUE.bits, 1);
        });
        assertTrue(e.getMessage().contains("Node variant is unknown"));
    }
}

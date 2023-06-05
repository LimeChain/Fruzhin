package com.limechain.internal.tree.decoder;

import com.limechain.internal.Variant;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import org.junit.jupiter.api.Test;

import static com.limechain.internal.tree.decoder.BranchDecoder.decodeBranch;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BranchDecoderTest {
    @Test
    public void decodeEmptyBranchExceptionTest() {
        Exception e = assertThrows(TrieDecoderException.class, () -> {
            byte[] data = new byte[]{};
            ScaleCodecReader reader = new ScaleCodecReader(data);
            decodeBranch(reader, (byte) Variant.BRANCH.bits, 1);
        });
        assertTrue(e.getMessage().contains("Could not decode partial key"));
    }

    @Test
    public void decodeBranchChildrenBitmapExceptionTest() {
        Exception e = assertThrows(TrieDecoderException.class, () -> {
            byte[] data = new byte[]{9};
            ScaleCodecReader reader = new ScaleCodecReader(data);
            decodeBranch(reader, (byte) Variant.BRANCH.bits, 1);
        });
        assertTrue(e.getMessage().contains("Could not decode children bitmap"));
    }

    @Test
    public void decodeBranchChildrenExceptionTest() {
        Exception e = assertThrows(TrieDecoderException.class, () -> {
            byte[] data = new byte[]{9, 0, 4};
            ScaleCodecReader reader = new ScaleCodecReader(data);
            decodeBranch(reader, (byte) Variant.BRANCH.bits, 1);
        });
        assertTrue(e.getMessage().contains("Could not decode child hash"));
    }

    @Test
    public void storageReadExceptionTest() {
        Exception e = assertThrows(TrieDecoderException.class, () -> {
            byte[] data = new byte[]{9, 0, 4};
            ScaleCodecReader reader = new ScaleCodecReader(data);
            decodeBranch(reader, (byte) Variant.BRANCH_WITH_VALUE.bits, 1);
        });
        assertTrue(e.getMessage().contains("Could not decode storage value"));
    }

    @Test
    public void emptyInlineNodeReadExceptionTest() {
        Exception e = assertThrows(TrieDecoderException.class, () -> {
            byte[] data = new byte[]{9, 0b0000_0001, 0b0000_0000, 4, 1, 4, 0};
            ScaleCodecReader reader = new ScaleCodecReader(data);
            decodeBranch(reader, (byte) Variant.BRANCH_WITH_VALUE.bits, 1);
        });
        assertTrue(e.getMessage().contains("Node variant is unknown"));
    }
}

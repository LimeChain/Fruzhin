package com.limechain.internal.tree.decoder;

import com.limechain.internal.Node;
import com.limechain.internal.Variant;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import lombok.extern.java.Log;

import static com.limechain.internal.tree.decoder.BranchDecoder.decodeBranch;
import static com.limechain.internal.tree.decoder.HeaderDecoder.decodeHeader;
import static com.limechain.internal.tree.decoder.LeafDecoder.decodeLeaf;

@Log
public class TreeDecoder {
    public static Node decode(ScaleCodecReader reader) throws TrieDecoderException {
        HeaderDecoder decodeHeaderResult = decodeHeader(reader);
        int variant = decodeHeaderResult.getVariantBits() & 0xff;
        int partialKeyLength = decodeHeaderResult.partialKeyLengthHeader;
        if (variant == Variant.LEAF.bits) {
            return decodeLeaf(reader, partialKeyLength);
        }
        if (variant == Variant.BRANCH.bits || variant == Variant.BRANCH_WITH_VALUE.bits) {
            return decodeBranch(reader, (byte) variant, partialKeyLength);
        }

        throw new TrieDecoderException("Unknown variant: " + variant);
    }
}

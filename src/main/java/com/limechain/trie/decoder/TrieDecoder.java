package com.limechain.trie.decoder;

import com.limechain.trie.Node;
import com.limechain.trie.NodeVariant;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import lombok.extern.java.Log;

import static com.limechain.trie.decoder.TrieHeaderDecoder.decodeHeader;

@Log
public class TrieDecoder {
    /**
     * Decodes encoded node data and its children recursively from a byte array.
     *
     * @param encoded a byte array containing the encoded node data
     * @return the decoded Node object
     * @throws TrieDecoderException if the variant does not match known variants
     */
    public static Node decode(byte[] encoded) throws TrieDecoderException {
        ScaleCodecReader reader = new ScaleCodecReader(encoded);
        TrieHeaderDecoderResult header = decodeHeader(reader);
        int variant = header.getVariantBits() & 0xff;
        int partialKeyLength = header.getPartialKeyLengthHeader();
        if (variant == NodeVariant.LEAF.bits) {
            return TrieLeafDecoder.decode(reader, partialKeyLength);
        }
        if (variant == NodeVariant.BRANCH.bits || variant == NodeVariant.BRANCH_WITH_VALUE.bits) {
            return TrieBranchDecoder.decode(reader, (byte) variant, partialKeyLength);
        }

        throw new TrieDecoderException("Unknown variant: " + variant);
    }
}

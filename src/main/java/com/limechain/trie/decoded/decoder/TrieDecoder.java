package com.limechain.trie.decoded.decoder;

import com.limechain.exception.trie.TrieDecoderException;
import com.limechain.trie.decoded.Node;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import lombok.experimental.UtilityClass;

import static com.limechain.trie.decoded.decoder.TrieHeaderDecoder.decodeHeader;

/**
 * This class is used to decode nodes and their children from a byte array.
 * <p>
 * Inspired by Gossamer’s implementation approach
 */
@UtilityClass
public class TrieDecoder {
    /**
     * Decodes encoded node data and its children recursively from a byte array.
     *
     * @param encoded a byte array containing the encoded node data
     * @return the decoded Node object
     * @throws TrieDecoderException if the variant does not match known variants
     */
    public static Node decode(byte[] encoded) {
        ScaleCodecReader reader = new ScaleCodecReader(encoded);
        TrieHeaderDecoderResult header = decodeHeader(reader);
        switch (header.nodeVariant()) {
            case EMPTY -> {
                return null;
            }
            case LEAF, LEAF_WITH_HASHED_VALUE -> {
                return TrieLeafDecoder.decode(reader, header.nodeVariant(), header.partialKeyLengthHeader());
            }
            case BRANCH, BRANCH_WITH_VALUE, BRANCH_WITH_HASHED_VALUE -> {
                return TrieBranchDecoder.decode(reader, header.nodeVariant(), header.partialKeyLengthHeader());
            }
            default -> throw new TrieDecoderException("Unknown variant: " + header.nodeVariant());
        }
    }
}

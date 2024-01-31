package com.limechain.trie.decoded.decoder;

import com.limechain.trie.decoded.NodeVariant;

/**
 * This class is used to store the data returned from the TrieHeaderDecoder
 */
public record TrieHeaderDecoderResult(NodeVariant nodeVariant, int partialKeyLengthHeader) {
}

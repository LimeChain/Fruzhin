package com.limechain.trie.decoder;

import com.limechain.trie.NodeVariant;

/**
 * This class is used to store the data returned from the TrieHeaderDecoder
 */
public record TrieHeaderDecoderResult(NodeVariant nodeVariant, int partialKeyLengthHeader) {
}

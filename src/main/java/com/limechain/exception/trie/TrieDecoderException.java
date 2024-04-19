package com.limechain.exception.trie;

/**
 * Custom exception class that is thrown when an error occurs
 * while decoding a Trie.
 */
public class TrieDecoderException extends RuntimeException {
    public TrieDecoderException(String errorMessage) {
        super(errorMessage);
    }
}

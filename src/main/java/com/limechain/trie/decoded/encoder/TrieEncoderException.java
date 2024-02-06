package com.limechain.trie.decoded.encoder;

/**
 * Custom exception class that is thrown when an error occurs
 * while encoding a Trie.
 */
public class TrieEncoderException extends RuntimeException {
    public TrieEncoderException(String errorMessage) {
        super(errorMessage);
    }
}

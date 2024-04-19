package com.limechain.exception.trie;

/**
 * Custom exception class that is thrown when an error occurs
 * while encoding a Trie.
 */
public class TrieEncoderException extends RuntimeException {
    public TrieEncoderException(String errorMessage) {
        super(errorMessage);
    }
}

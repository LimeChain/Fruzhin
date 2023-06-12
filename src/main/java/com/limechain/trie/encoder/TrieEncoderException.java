package com.limechain.trie.encoder;

/**
 * Custom exception class that is thrown when an error occurs
 * while encoding a Trie.
 */
public class TrieEncoderException extends Exception {
    public TrieEncoderException(String errorMessage) {
        super(errorMessage);
    }
}

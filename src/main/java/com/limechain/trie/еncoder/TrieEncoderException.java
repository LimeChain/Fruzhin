package com.limechain.trie.еncoder;

/**
 * Custom exception class that is thrown when an error occurs
 * while encoding a Trie.
 */
public class TrieEncoderException extends Exception {
    public TrieEncoderException(String errorMessage) {
        super(errorMessage);
    }
}

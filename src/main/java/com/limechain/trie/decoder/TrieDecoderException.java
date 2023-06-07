package com.limechain.trie.decoder;

/**
 * Custom exception class that is thrown when an error occurs
 * while decoding a Trie.
 */
public class TrieDecoderException extends Exception {
    public TrieDecoderException(String errorMessage) {
        super(errorMessage);
    }
}

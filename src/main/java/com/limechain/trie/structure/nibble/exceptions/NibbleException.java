package com.limechain.trie.structure.nibble.exceptions;

public class NibbleException extends RuntimeException {
    public NibbleException(Throwable cause) {
        super(cause);
    }

    public NibbleException(String message) {
        super(message);
    }

    public NibbleException(String message, Throwable cause) {
        super(message, cause);
    }
}

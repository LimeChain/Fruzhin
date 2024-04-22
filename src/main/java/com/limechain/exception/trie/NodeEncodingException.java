package com.limechain.exception.trie;

public class NodeEncodingException extends RuntimeException {
    public NodeEncodingException(Throwable cause) {
        super(cause);
    }

    public NodeEncodingException(String message) {
        super(message);
    }

    public NodeEncodingException(String message, Throwable cause) {
        super(message, cause);
    }

}

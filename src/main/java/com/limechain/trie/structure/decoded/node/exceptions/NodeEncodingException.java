package com.limechain.trie.structure.decoded.node.exceptions;

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

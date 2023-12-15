package com.limechain.exception;

public class PeerNotFoundException extends RuntimeException {
    public PeerNotFoundException(String message) {
        super(message);
    }
}

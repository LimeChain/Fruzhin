package com.limechain.exception.rpc;

public class PeerNotFoundException extends RuntimeException {
    public PeerNotFoundException(String message) {
        super(message);
    }
}

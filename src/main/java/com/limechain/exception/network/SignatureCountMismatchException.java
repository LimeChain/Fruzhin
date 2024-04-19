package com.limechain.exception.network;

public class SignatureCountMismatchException extends RuntimeException {
    public SignatureCountMismatchException(String message) {
        super(message);
    }
}

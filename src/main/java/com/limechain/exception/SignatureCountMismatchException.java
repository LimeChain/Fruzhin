package com.limechain.exception;

public class SignatureCountMismatchException extends RuntimeException {
    public SignatureCountMismatchException(String message) {
        super(message);
    }
}

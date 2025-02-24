package com.limechain.exception.sync;

public class JustificationVerificationException extends RuntimeException {
    public JustificationVerificationException(String message) {
        super(message);
    }

    public JustificationVerificationException(String message, Throwable cause) {
        super(message, cause);
    }
}

package com.limechain.exception;

public class CliArgsParseException extends RuntimeException {
    public CliArgsParseException(String message, Throwable cause) {
        super(message, cause);
    }
}

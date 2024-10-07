package com.limechain.exception.hostapi;

public class InvalidArgumentException extends RuntimeException {
    public InvalidArgumentException(String argument, Object value) {
        super("Invalid " + argument + ": " + value);
    }
}

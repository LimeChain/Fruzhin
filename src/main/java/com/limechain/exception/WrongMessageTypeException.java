package com.limechain.exception;

public class WrongMessageTypeException extends RuntimeException {
    public WrongMessageTypeException(String message) {
        super(message);
    }
}

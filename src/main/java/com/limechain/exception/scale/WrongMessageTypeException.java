package com.limechain.exception.scale;

public class WrongMessageTypeException extends RuntimeException {
    public WrongMessageTypeException(String message) {
        super(message);
    }
}

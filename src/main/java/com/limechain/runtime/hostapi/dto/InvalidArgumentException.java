package com.limechain.runtime.hostapi.dto;

public class InvalidArgumentException extends RuntimeException{
    public InvalidArgumentException(String argument, Object value) {
        super("Invalid " + argument + ": " + value);
    }
}

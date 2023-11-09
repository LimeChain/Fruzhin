package com.limechain.storage.block.exception;

public class LowerThanRootException extends RuntimeException{
    public LowerThanRootException(String message) {
        super(message);
    }
}

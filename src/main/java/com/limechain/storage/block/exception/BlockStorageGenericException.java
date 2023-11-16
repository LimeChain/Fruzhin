package com.limechain.storage.block.exception;

public class BlockStorageGenericException extends RuntimeException{
    public BlockStorageGenericException(String message) {
        super(message);
    }

    public BlockStorageGenericException(String message, Throwable cause) {
        super(message, cause);
    }
}

package com.limechain.exception.storage;

public class BlockNodeNotFoundException extends BlockStorageGenericException {
    public BlockNodeNotFoundException(String message) {
        super(message);
    }

    public BlockNodeNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

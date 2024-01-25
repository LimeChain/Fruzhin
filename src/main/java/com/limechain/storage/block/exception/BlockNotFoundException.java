package com.limechain.storage.block.exception;

public class BlockNotFoundException extends BlockStorageGenericException {
    public BlockNotFoundException(String message) {
        super(message);
    }

    public BlockNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

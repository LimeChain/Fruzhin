package com.limechain.exception.storage;

public class BlockAlreadyExistsException extends BlockStorageGenericException {
    public BlockAlreadyExistsException(String message) {
        super(message);
    }

    public BlockAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}

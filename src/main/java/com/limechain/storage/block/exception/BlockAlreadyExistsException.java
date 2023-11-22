package com.limechain.storage.block.exception;

public class BlockAlreadyExistsException extends BlockStorageGenericException {
    public BlockAlreadyExistsException(String message) {
        super(message);
    }

    public BlockAlreadyExistsException(String message, Throwable cause) {
        super(message, cause);
    }
}

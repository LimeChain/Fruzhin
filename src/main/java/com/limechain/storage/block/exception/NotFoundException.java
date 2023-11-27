package com.limechain.storage.block.exception;

public class NotFoundException extends BlockStorageGenericException {
    public NotFoundException(String message) {
        super(message);
    }

    public NotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

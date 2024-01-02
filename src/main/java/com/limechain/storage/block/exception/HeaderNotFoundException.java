package com.limechain.storage.block.exception;

public class HeaderNotFoundException extends BlockStorageGenericException {
    public HeaderNotFoundException(String message) {
        super(message);
    }

    public HeaderNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

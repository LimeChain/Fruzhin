package com.limechain.exception.storage;

public class LowerThanRootException extends BlockStorageGenericException {
    public LowerThanRootException(String message) {
        super(message);
    }

    public LowerThanRootException(String message, Throwable cause) {
        super(message, cause);
    }
}

package com.limechain.storage.block.exception;

public class BlockNotFoundException extends NotFoundException {
    public BlockNotFoundException(String message) {
        super(message);
    }

    public BlockNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

package com.limechain.storage.block.exception;

public class BlockNodeNotFoundException extends NotFoundException{
    public BlockNodeNotFoundException(String message) {
        super(message);
    }

    public BlockNodeNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}

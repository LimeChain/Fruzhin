package com.limechain.exception.global;

public class ExecutionFailedException extends RuntimeException {
    public ExecutionFailedException(Throwable e) {
        super(e);
    }

    public ExecutionFailedException(String message) {
        super(message);
    }
}

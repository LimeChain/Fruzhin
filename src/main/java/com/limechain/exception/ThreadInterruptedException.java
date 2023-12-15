package com.limechain.exception;

public class ThreadInterruptedException extends RuntimeException {
    public ThreadInterruptedException(Throwable cause) {
        super(cause);
    }
}

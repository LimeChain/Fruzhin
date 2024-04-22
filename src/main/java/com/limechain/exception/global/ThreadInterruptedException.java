package com.limechain.exception.global;

public class ThreadInterruptedException extends RuntimeException {
    public ThreadInterruptedException(Throwable cause) {
        super(cause);
    }
}

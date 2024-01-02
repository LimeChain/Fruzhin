package com.limechain.exception;

public class NotificationFailedException extends RuntimeException {
    public NotificationFailedException(Throwable e) {
        super(e);
    }
}

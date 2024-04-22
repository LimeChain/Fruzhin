package com.limechain.exception.rpc;

public class NotificationFailedException extends RuntimeException {
    public NotificationFailedException(Throwable e) {
        super(e);
    }
}

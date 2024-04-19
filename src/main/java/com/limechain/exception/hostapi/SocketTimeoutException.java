package com.limechain.exception.hostapi;

public class SocketTimeoutException extends RuntimeException {
    public SocketTimeoutException (String msg) {
        super(msg);
    }
}

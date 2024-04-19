package com.limechain.exception.hostapi;

public class OffchainResponseWaitException extends RuntimeException {
    public OffchainResponseWaitException(Throwable e) {
        super(e);
    }
}
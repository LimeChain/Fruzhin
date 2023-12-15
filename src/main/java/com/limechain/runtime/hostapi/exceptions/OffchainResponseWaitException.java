package com.limechain.runtime.hostapi.exceptions;

public class OffchainResponseWaitException extends RuntimeException {
    public OffchainResponseWaitException(Throwable e) {
        super(e);
    }
}
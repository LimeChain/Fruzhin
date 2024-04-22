package com.limechain.exception.rpc;

public class WsMessageSendException extends RuntimeException {
    public WsMessageSendException(String message) {
        super(message);
    }
}

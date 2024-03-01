package com.limechain.rpc.exceptions;

public class WsMessageSendException extends RuntimeException {
    public WsMessageSendException(String message) {
        super(message);
    }
}

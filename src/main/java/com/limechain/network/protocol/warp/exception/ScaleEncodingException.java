package com.limechain.network.protocol.warp.exception;

public class ScaleEncodingException extends RuntimeException{
    public ScaleEncodingException(Throwable cause) {
        super(cause);
    }

    public ScaleEncodingException(String message) {
        super(message);
    }

    public ScaleEncodingException(String message, Throwable cause) {
        super(message, cause);
    }
}
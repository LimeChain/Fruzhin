package com.limechain.exception.scale;

public class ScaleDecodingException extends RuntimeException {
    public ScaleDecodingException(Throwable cause) {
        super(cause);
    }

    public ScaleDecodingException(String message) {
        super(message);
    }

    public ScaleDecodingException(String message, Throwable cause) {
        super(message, cause);
    }
}

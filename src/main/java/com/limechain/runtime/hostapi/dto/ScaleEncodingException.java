package com.limechain.runtime.hostapi.dto;

public class ScaleEncodingException extends RuntimeException {
    public ScaleEncodingException(Throwable cause) {
        super("Scale encoding failed!", cause);
    }
}

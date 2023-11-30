package com.limechain.exception;

public class WasmRuntimeException extends RuntimeException {
    public WasmRuntimeException(String message) {
        super(message);
    }
}

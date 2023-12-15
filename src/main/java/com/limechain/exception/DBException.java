package com.limechain.exception;

public class DBException extends RuntimeException {
    public DBException(Throwable e) {
        super(e);
    }
}

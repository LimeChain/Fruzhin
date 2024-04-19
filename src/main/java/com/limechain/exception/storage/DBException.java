package com.limechain.exception.storage;

public class DBException extends RuntimeException {
    public DBException(Throwable e) {
        super(e);
    }
}

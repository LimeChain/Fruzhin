package com.limechain.exception;

public class InvalidNodeRoleException extends RuntimeException {
    public InvalidNodeRoleException() {
        super();
    }

    public InvalidNodeRoleException(String message) {
        super(message);
    }
}

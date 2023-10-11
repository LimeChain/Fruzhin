package com.limechain.runtime.allocator;

public class AllocationError extends RuntimeException {
    public AllocationError(String message) {
        super(message);
    }
}

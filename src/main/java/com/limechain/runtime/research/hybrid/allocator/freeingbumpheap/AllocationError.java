package com.limechain.runtime.research.hybrid.allocator.freeingbumpheap;

public class AllocationError extends RuntimeException {
    public AllocationError(String message) {
        super(message);
    }
}

package com.limechain.runtime.allocator;

import lombok.Data;

import java.math.BigInteger;

@Data
public class AllocationStats {
    private int bytesAllocated;
    private int bytesAllocatedPeak;
    private BigInteger bytesAllocatedSum = BigInteger.ZERO;
    private int addressSpaceUsed;

    public void allocated(int allocatedSize, int addressSpaceUsed) {
        bytesAllocated += allocatedSize;
        bytesAllocatedSum = bytesAllocatedSum.add(BigInteger.valueOf(allocatedSize));
        bytesAllocatedPeak = Math.max(bytesAllocatedPeak, bytesAllocated);
        this.addressSpaceUsed = addressSpaceUsed;
    }

    public void deallocated(int deallocateSize) {
        bytesAllocated -= deallocateSize;
    }
}

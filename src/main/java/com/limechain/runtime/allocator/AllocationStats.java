package com.limechain.runtime.allocator;

import lombok.Data;

import java.math.BigInteger;

@Data
public class AllocationStats {
    private int bytesAllocated;
    private int bytesAllocatedPeak;
    private BigInteger bytesAllocatedSum;
    private int addressSpaceUsed;
}

package com.limechain.runtime.allocator;

import com.limechain.runtime.hostapi.dto.RuntimePointerSize;
import com.limechain.runtime.memory.Memory;

/**
 * A simple minimalistic interface for an allocator over some {@link Memory}.
 */
public interface Allocator {
    RuntimePointerSize allocate(int size, Memory memory);
    void deallocate(int pointer, Memory memory);
}

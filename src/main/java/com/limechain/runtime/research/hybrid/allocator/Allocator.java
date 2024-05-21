package com.limechain.runtime.research.hybrid.allocator;

import com.limechain.runtime.hostapi.dto.RuntimePointerSize;
import com.limechain.runtime.research.hybrid.memory.Memory;

// TODO: Abstract `org.wasmer.WasmMemory` into a new wrapper type
public interface Allocator {
    RuntimePointerSize allocate(int size, Memory memory);
    void deallocate(int pointer, Memory memory);
}

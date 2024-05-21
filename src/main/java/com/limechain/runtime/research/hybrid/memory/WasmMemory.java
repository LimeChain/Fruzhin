package com.limechain.runtime.research.hybrid.memory;

import java.nio.ByteBuffer;

public record WasmMemory(org.wasmer.Memory memory) implements Memory {
    @Override
    public ByteBuffer buffer() {
        return memory.buffer();
    }

    @Override
    public int grow(int page) {
        return memory.grow(page);
    }
}

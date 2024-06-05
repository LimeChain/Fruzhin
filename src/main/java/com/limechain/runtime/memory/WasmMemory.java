package com.limechain.runtime.memory;

import java.nio.ByteBuffer;

/**
 * A new type wrapping {@link org.wasmer.Memory} and implementing {@link Memory}.
 * Introduced purely for decoupling.
 * @param memory the underlying {@link org.wasmer.Memory}
 */
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

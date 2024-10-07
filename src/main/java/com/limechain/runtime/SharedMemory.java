package com.limechain.runtime;

import com.limechain.runtime.allocator.Allocator;
import com.limechain.runtime.hostapi.dto.RuntimePointerSize;
import com.limechain.runtime.memory.Memory;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Setter;

import java.nio.ByteBuffer;

/**
 * A container for a {@link Memory} with an added {@link Allocator}, providing memory management functionality.
 */
@Setter(AccessLevel.PACKAGE)
@AllArgsConstructor
public class SharedMemory {
    private Memory memory;
    private Allocator allocator;

    /**
     * Get the data stored in memory using a {@link  RuntimePointerSize}
     *
     * @param runtimePointerSize pointer to data and its size
     * @return byte array with read data
     */
    public byte[] readData(RuntimePointerSize runtimePointerSize) {
        ByteBuffer memoryBuffer = this.memory.buffer();
        byte[] data = new byte[runtimePointerSize.size()];
        memoryBuffer.position(runtimePointerSize.pointer());
        memoryBuffer.get(data);
        return data;
    }

    /**
     * Write data to memory, by allocating space in memory and then writing to it.
     *
     * @param data data to be written
     * @return a pointer size to the written data
     */
    public RuntimePointerSize writeData(byte[] data) {
        RuntimePointerSize allocatedPointer = allocate(data.length);
        writeData(data, allocatedPointer);
        return allocatedPointer;
    }

    /**
     * Write data to memory, by using a {@link RuntimePointerSize}.
     * <br>Data will be written at the given {@link RuntimePointerSize#pointer() pointer}.
     * <br>Only the first bytes up to the given {@link RuntimePointerSize#size() size} will be written.
     *
     * @param data               data to be written to memory
     * @param runtimePointerSize pointer to memory and size of data to be stored.
     */
    public void writeData(byte[] data, RuntimePointerSize runtimePointerSize) {
        ByteBuffer memoryBuffer = memory.buffer();
        memoryBuffer.position(runtimePointerSize.pointer());
        memoryBuffer.put(data, 0, Math.min(data.length, runtimePointerSize.size()));
    }

    /**
     * Allocate a number of bytes in memory.
     *
     * @param numberOfBytes number of bytes to be allocated
     * @return a pointer-size to the allocated space in memory
     */
    public RuntimePointerSize allocate(int numberOfBytes) {
        return allocator.allocate(numberOfBytes, memory);
    }

    /**
     * Deallocate the space at given memory pointer
     *
     * @param pointer position in memory
     */
    public void deallocate(int pointer) {
        allocator.deallocate(pointer, memory);
    }
}

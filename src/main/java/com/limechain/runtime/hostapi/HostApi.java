package com.limechain.runtime.hostapi;

import com.limechain.runtime.Runtime;
import com.limechain.runtime.allocator.FreeingBumpHeapAllocator;
import com.limechain.runtime.hostapi.dto.RuntimePointerSize;
import lombok.extern.java.Log;
import org.wasmer.ImportObject;
import org.wasmer.Type;

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Holds common methods and services used by the different
 * HostApi functions implementations
 */
@Log
public class HostApi {
    protected static final List<Number> EMPTY_LIST_OF_NUMBER = List.of();
    protected static final List<Type> EMPTY_LIST_OF_TYPES = List.of();
    private final Runtime runtime;
    private FreeingBumpHeapAllocator allocator;

    public HostApi(final Runtime runtime) {
        this.runtime = runtime;
    }

    protected static ImportObject getImportObject(final String functionName,
                                                  final Function<List<Number>, Number> function,
                                                  final List<Type> args,
                                                  final Type retType) {
        return new ImportObject.FuncImport("env", functionName, argv -> {
            log.fine(String.format("Message printed in the body of '%s'%n", functionName));
            return Collections.singletonList(function.apply(argv));
        }, args, Collections.singletonList(retType));
    }

    protected static ImportObject getImportObject(final String functionName,
                                                  final Consumer<List<Number>> function,
                                                  final List<Type> args) {
        return new ImportObject.FuncImport("env", functionName, argv -> {
            log.fine(String.format("Message printed in the body of '%s'%n", functionName));
            function.accept(argv);
            return EMPTY_LIST_OF_NUMBER;
        }, args, EMPTY_LIST_OF_TYPES);
    }

    public void updateAllocator() {
        this.allocator = new FreeingBumpHeapAllocator(runtime.getHeapBase());
    }

    /**
     * Get the data stored in memory using a {@link  RuntimePointerSize}
     *
     * @param runtimePointerSize pointer to data and its size
     * @return byte array with read data
     */
    public byte[] getDataFromMemory(RuntimePointerSize runtimePointerSize) {
        return runtime.getDataFromMemory(runtimePointerSize);
    }

    /**
     * Write data to memory, by allocating space in memory and then writing to it.
     *
     * @param data data to be written
     * @return a pointer size to the written data
     */
    public RuntimePointerSize writeDataToMemory(byte[] data) {
        RuntimePointerSize allocatedPointer = allocate(data.length);
        writeDataToMemory(data, allocatedPointer);
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
    public void writeDataToMemory(byte[] data, RuntimePointerSize runtimePointerSize) {
        ByteBuffer memoryBuffer = runtime.getMemory().buffer();
        memoryBuffer.position(runtimePointerSize.pointer());
        memoryBuffer.put(data, 0, runtimePointerSize.size());
    }

    /**
     * Allocate a number of bytes in memory.
     *
     * @param numberOfBytes number of bytes to be allocated
     * @return a pointer-size to the allocated space in memory
     */
    public RuntimePointerSize allocate(int numberOfBytes) {
        return allocator.allocate(numberOfBytes, runtime.getMemory());
    }

    /**
     * Deallocate the space at given memory pointer
     *
     * @param pointer position in memory
     */
    public void deallocate(int pointer) {
        allocator.deallocate(pointer, runtime.getMemory());
    }
}

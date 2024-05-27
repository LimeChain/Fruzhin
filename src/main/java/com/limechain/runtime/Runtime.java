package com.limechain.runtime;

import com.limechain.runtime.allocator.FreeingBumpHeapAllocator;
import com.limechain.runtime.hostapi.WasmExports;
import com.limechain.runtime.hostapi.dto.RuntimePointerSize;
import com.limechain.runtime.version.RuntimeVersion;
import lombok.Getter;
import lombok.extern.java.Log;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wasmer.ImportObject;
import org.wasmer.Imports;
import org.wasmer.Instance;
import org.wasmer.Memory;
import org.wasmer.Module;

import java.nio.ByteBuffer;
import java.util.logging.Level;

import static com.limechain.runtime.RuntimeBuilder.getImports;

@Getter
@Log
public class Runtime {
    private RuntimeVersion version;
    private final Instance instance;
    private final int heapPages;
    private final FreeingBumpHeapAllocator allocator;

    public Runtime(Module module, ImportObject.MemoryImport memoryImport, int heapPages) {
        this.heapPages = heapPages;
        this.instance = module.instantiate(Imports.from(getImports(memoryImport, this), module));
        this.allocator = new FreeingBumpHeapAllocator(getHeapBase());
    }

    /**
     * Calls an exported runtime function with no parameters.
     * @param functionName the name of the function
     * @return the SCALE encoded response
     */
    @Nullable
    public byte[] call(String functionName) {
        return callInner(functionName, new RuntimePointerSize(0, 0));
    }

    /**
     * Calls an exported runtime function with parameters.
     * @param functionName the name of the function
     * @param parameter the SCALE encoded tuple of parameters
     * @return the SCALE encoded response
     */
    @Nullable
    public byte[] call(String functionName, @NotNull byte[] parameter) {
        return callInner(functionName, writeDataToMemory(parameter));
    }

    @Nullable
    private byte[] callInner(String functionName, RuntimePointerSize parameterPtrSize) {
        log.log(Level.FINE, "Making a runtime call: " + functionName);
        Object[] response = instance.exports.getFunction(functionName)
            .apply(parameterPtrSize.pointer(), parameterPtrSize.size());

        if (response == null) {
            return null;
        }

        RuntimePointerSize responsePtrSize = new RuntimePointerSize((long) response[0]);
        return getDataFromMemory(responsePtrSize);
    }

    /**
     * This setter exists to be used only in the RuntimeBuilder, since the RuntimeVersion is essentially an attribute
     * of the Runtime, thus modeled as its field. If we can't find it in a custom section directly from the binary
     * though, we'll still need an instance of `Runtime` in order to obtain the version by calling `Core_version`,
     * and set it afterward via this setter.
     */
    void setVersion(RuntimeVersion runtimeVersion) {
        this.version = runtimeVersion;
    }

    public int getHeapBase() {
        return instance.exports.getGlobal(WasmExports.HEAP_BASE.getValue()).getIntValue();
    }

    public int getDataEnd() {
        return instance.exports.getGlobal(WasmExports.DATA_END.getValue()).getIntValue();
    }

    private Memory getMemory() {
        return instance.exports.getMemory(WasmExports.MEMORY.getValue());
    }

    /**
     * Get the data stored in memory using a {@link  RuntimePointerSize}
     *
     * @param runtimePointerSize pointer to data and its size
     * @return byte array with read data
     */
    public byte[] getDataFromMemory(RuntimePointerSize runtimePointerSize) {
        ByteBuffer memoryBuffer = this.getMemory().buffer();
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
        ByteBuffer memoryBuffer = getMemory().buffer();
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
        return allocator.allocate(numberOfBytes, getMemory());
    }

    /**
     * Deallocate the space at given memory pointer
     *
     * @param pointer position in memory
     */
    public void deallocate(int pointer) {
        allocator.deallocate(pointer, getMemory());
    }
}

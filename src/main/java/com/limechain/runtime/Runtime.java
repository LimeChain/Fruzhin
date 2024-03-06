package com.limechain.runtime;

import com.limechain.runtime.allocator.FreeingBumpHeapAllocator;
import com.limechain.runtime.hostapi.HostApi;
import com.limechain.runtime.hostapi.WasmExports;
import com.limechain.runtime.hostapi.dto.RuntimePointerSize;
import com.limechain.runtime.version.RuntimeVersion;
import lombok.Getter;
import lombok.extern.java.Log;
import org.jetbrains.annotations.Nullable;
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
    private Instance instance;
    private int heapPages;
    private FreeingBumpHeapAllocator allocator;

    public Runtime(Module module, int heapPages) {
        this.heapPages = heapPages;
        HostApi hostApi = new HostApi(this);
        this.instance = module.instantiate(getImports(module, hostApi));
        hostApi.updateAllocator();
        this.allocator = new FreeingBumpHeapAllocator(getHeapBase());
    }

    // TODO: Add adequate parameters to the runtime call
    @Nullable
    public byte[] call(String functionName) {
        log.log(Level.INFO, "Making a runtime call: " + functionName);
        Object[] response = instance.exports
            .getFunction(functionName)
            .apply(0, 0);

        if (response == null) {
            return null;
        }

        RuntimePointerSize responsePtrSize = new RuntimePointerSize((long) response[0]);
        return getDataFromMemory(responsePtrSize);
    }

    @Nullable
    public byte[] callWithArgs(String functionName, RuntimePointerSize rps) {
        log.log(Level.INFO, "Making a runtime call: " + functionName);
        Object[] response = instance.exports
                .getFunction(functionName)
                .apply(rps.pointer(), rps.size());

        if (response == null) {
            return null;
        }

        RuntimePointerSize responsePtrSize = new RuntimePointerSize((long) response[0]);
        return getDataFromMemory(responsePtrSize);
    }

    /**
     * This setter exists to be used only in the RuntimeBuilder, since the RuntimeVersion is essentially an attribute
     * of the Runtime, thus modeled as its field. If we can't find it in a custom section directly from the binary
     * though, we'll still need an instance of `Runtime` in order to obtain the version, and set it afterward via
     * this setter.
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

    public Memory getMemory() {
        return instance.exports.getMemory(WasmExports.MEMORY.getValue());
    }

    // TODO: Think about moving `writeDataToMemory` from `HostApi` into here, too...
    //  for now, only the reading has been moved (as deemed necessary to be here)
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

    public RuntimePointerSize writeDataToMemory(byte[] data) {
        RuntimePointerSize allocatedPointer = allocate(data.length);
        writeDataToMemory(data, allocatedPointer);
        return allocatedPointer;
    }

    public void writeDataToMemory(byte[] data, RuntimePointerSize runtimePointerSize) {
        ByteBuffer memoryBuffer = getMemory().buffer();
        memoryBuffer.position(runtimePointerSize.pointer());
        memoryBuffer.put(data, 0, runtimePointerSize.size());
    }

    public RuntimePointerSize allocate(int numberOfBytes) {
        return allocator.allocate(numberOfBytes, getMemory());
    }
}

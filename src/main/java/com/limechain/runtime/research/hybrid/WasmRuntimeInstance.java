package com.limechain.runtime.research.hybrid;

import com.limechain.runtime.hostapi.WasmExports;
import com.limechain.runtime.hostapi.dto.RuntimePointerSize;
import com.limechain.runtime.research.hybrid.allocator.Allocator;
import com.limechain.runtime.research.hybrid.allocator.freeingbumpheap.FreeingBumpHeapAllocator;
import com.limechain.runtime.research.hybrid.context.Context;
import com.limechain.runtime.research.hybrid.hostapi.MinimalHostapiImpl;
import com.limechain.runtime.research.hybrid.hostapi.SharedMemory;
import com.limechain.runtime.research.hybrid.memory.WasmMemory;
import com.limechain.runtime.version.RuntimeVersion;
import lombok.extern.java.Log;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wasmer.ImportObject;
import org.wasmer.Imports;
import org.wasmer.Instance;
import org.wasmer.Memory;
import org.wasmer.Module;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;


@Log
public class WasmRuntimeInstance {
    Module module; // used to open/close all its instances

    Instance instance;
    Context context;

    Allocator allocator;

    private RuntimeVersion version;

    // TODO: Reconsider whether `Context` should be a constructor argument or (dependency injected)
    //                       OR an argument of `call()` (dependency parameterized)
    //  The former necessitates the explicit passing of a context for every build of the runtime (we can't build the underlying module only - we always instantiate and tie the instance to the context)
    //          - In more lightweight cases (i.e. only build to fetch the version), we'll have to explicitly provide a "partial" context; "partial" meaning with some of the services inside being null (or partially implemented) since we know they are not going to be used)
    //  The latter necessitates the explicit re-instantiating of the underlying `org.wasmer.Instance` for each RuntimeAPI call (they do that in Gossamer), but allows for easy caching of the compiled wasm module (thus we can pass that around, e.g. in the BlockState) and instantiate with a context only when invocation is necessary
    public WasmRuntimeInstance(Module module, Context context) {
        this.context = context;

        SharedMemory sharedMemory = new SharedMemory(null, null);
        context.setSharedMemory(sharedMemory);

        // getting imports
        ImportObject.MemoryImport memory = new ImportObject.MemoryImport("env", 22, false);
        List<ImportObject> functionImports = new MinimalHostapiImpl(context).getFunctionImports(); // NOTE: We could even inject the "Implementation" object from outside, if we'd want more granularity

        List<ImportObject> imports = new ArrayList<>(functionImports);
        imports.add(memory);

        this.instance = module.instantiate(Imports.from(imports, module));
        this.allocator = new FreeingBumpHeapAllocator(this.getHeapBase());
        sharedMemory.setMemory(new WasmMemory(getMemory()));
        sharedMemory.setAllocator(this.allocator);
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
        return callInner(functionName, context.getSharedMemory().writeData(parameter));
    }

    @Nullable
    private byte[] callInner(String functionName, RuntimePointerSize parameterPtrSize) {
        log.log(Level.INFO, "Making a runtime call: " + functionName);
        Object[] response = instance.exports.getFunction(functionName)
            .apply(parameterPtrSize.pointer(), parameterPtrSize.size());

        if (response == null) {
            return null;
        }

        RuntimePointerSize responsePtrSize = new RuntimePointerSize((long) response[0]);
        return context.getSharedMemory().readData(responsePtrSize);
    }

    /**
     * This setter exists to be used only in the RuntimeBuilder, since the RuntimeVersion is essentially an attribute
     * of the Runtime, thus modeled as its field. If we can't find it in a custom section directly from the binary
     * though, we'll still need an instance of `Runtime` in order to obtain the version by calling `Core_version`,
     * and set it afterward via this setter.
     */
    void setVersion(@NotNull RuntimeVersion runtimeVersion) {
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
}

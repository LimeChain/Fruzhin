package com.limechain.runtime;

import com.limechain.runtime.hostapi.dto.RuntimePointerSize;
import com.limechain.runtime.version.RuntimeVersion;
import com.limechain.runtime.version.scale.RuntimeVersionReader;
import com.limechain.utils.scale.ScaleUtils;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.java.Log;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wasmer.Instance;
import org.wasmer.Module;

import java.util.logging.Level;

@Log
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class Runtime {
    // TODO: Figure out whether we'll actually need this field
    Module module; // used to open/close all of its own instances
    Context context;
    Instance instance;

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
        log.log(Level.FINE, "Making a runtime call: " + functionName);
        Object[] response = instance.exports.getFunction(functionName)
            .apply(parameterPtrSize.pointer(), parameterPtrSize.size());

        if (response == null) {
            return null;
        }

        RuntimePointerSize responsePtrSize = new RuntimePointerSize((long) response[0]);
        return context.getSharedMemory().readData(responsePtrSize);
    }

    /**
     * @return the {@link RuntimeVersion} of this runtime instance.
     * @implNote The runtime version is cached in the context, so no actual runtime invocation is performed.
     */
    public RuntimeVersion getVersion() {
        return this.context.getRuntimeVersion();
    }

    RuntimeVersion callCoreVersion() {
        return ScaleUtils.Decode.decode(this.call("Core_version"), new RuntimeVersionReader());
    }

    /**
     * Closes the underlying instance.
     */
    public void close() {
        // TODO:
        //  We're not sure whether we should also close the module.
        //  That's what Gossamer does though, but their wasm runtime could be doing things differently underneath.
        this.module.close();
        this.instance.close();
    }
}

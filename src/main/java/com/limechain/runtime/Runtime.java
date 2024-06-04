package com.limechain.runtime;

import com.limechain.runtime.hostapi.dto.RuntimePointerSize;
import com.limechain.runtime.version.RuntimeVersion;
import com.limechain.runtime.version.scale.RuntimeVersionReader;
import com.limechain.utils.scale.ScaleUtils;
import lombok.extern.java.Log;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.wasmer.Instance;
import org.wasmer.Module;

import java.util.logging.Level;

@Log
public class Runtime {
    // TODO: Figure out whether we'll actually need this field
    Module module; // used to open/close all of its own instances
    Context context;
    Instance instance;

    // TODO: Reconsider whether `Context` should be a constructor argument or (dependency injected)
    //                       OR an argument of `Runtime::call()` (dependency parameterized)
    //  The former necessitates the explicit passing of a context for every build of the runtime
    //      (we can't build the underlying module only - we always instantiate and tie the instance to the context)
    //    - In more lightweight cases (i.e. only build to fetch the version), we'll have to explicitly provide a "partial" context;
    //      "partial" meaning with some of the services inside being null (or partially implemented)
    //      since we know they are not going to be used
    //  The latter necessitates the explicit re-instantiating of the underlying `org.wasmer.Instance`
    //      for each RuntimeAPI call (they do that in Gossamer),
    //      but allows for easy caching of the compiled wasm module (thus we can pass that around, e.g. in the BlockState)
    //      and instantiate with a context only when invocation is necessary.
    //      This might prove more fitting, for example,
    //      when executing consecutive blocks (between which there's no runtime updates)
    //      without rebuilding the module and by only changing the context...
    Runtime(Module module, Context context, Instance instance) {
        this.module = module;
        this.context = context;
        this.instance = instance;
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

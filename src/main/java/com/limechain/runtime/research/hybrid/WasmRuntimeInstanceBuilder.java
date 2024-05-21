package com.limechain.runtime.research.hybrid;

import com.github.luben.zstd.Zstd;
import com.limechain.runtime.WasmSectionUtils;
import com.limechain.runtime.research.hybrid.context.Context;
import com.limechain.runtime.version.RuntimeVersion;
import com.limechain.runtime.version.scale.RuntimeVersionReader;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import lombok.extern.java.Log;
import org.wasmer.Module;

import java.util.Arrays;
import java.util.logging.Level;

@Log
public class WasmRuntimeInstanceBuilder {
    private static final byte[] ZSTD_PREFIX = new byte[] {82, -68, 83, 118, 70, -37, -114, 5};
    private static final int MAX_ZSTD_DECOMPRESSED_SIZE = 50 * 1024 * 1024;

    /**
     * Builds and returns a Runtime object for executing WebAssembly code.
     *
     * @param code The WebAssembly bytecode.
     * @return A Runtime object.
     */
    public WasmRuntimeInstance buildRuntime(byte[] code, Context context) {
        byte[] wasmBinary = zstDecompressIfNecessary(code);

        Module module = new Module(wasmBinary);
        WasmRuntimeInstance instance = new WasmRuntimeInstance(module, context);

        RuntimeVersion runtimeVersion = WasmSectionUtils.parseRuntimeVersionFromCustomSections(wasmBinary);

        //If we couldn't get the data from the wasm custom sections fallback to Core_version call
        if (runtimeVersion == null) {
            log.log(Level.INFO, "Couldn't fetch runtime version from custom section, calling 'Core_version'.");
            runtimeVersion = this.getRuntimeVersionFromInstance(instance);
        }

        instance.setVersion(runtimeVersion);
        return instance;
    }

    private byte[] zstDecompressIfNecessary(byte[] code) {
        byte[] wasmBinaryPrefix = Arrays.copyOfRange(code, 0, 8);
        if (Arrays.equals(wasmBinaryPrefix, ZSTD_PREFIX)) {
            return Zstd.decompress(
                Arrays.copyOfRange(code, ZSTD_PREFIX.length, code.length),
                MAX_ZSTD_DECOMPRESSED_SIZE
            );
        }

        return code;
    }

    private RuntimeVersion getRuntimeVersionFromInstance(WasmRuntimeInstance instance) {
        byte[] data = instance.call("Core_version");

        ScaleCodecReader reader = new ScaleCodecReader(data);
        RuntimeVersionReader runtimeVersionReader = new RuntimeVersionReader();
        return runtimeVersionReader.read(reader);
    }
}
package com.limechain.runtime;

import com.github.luben.zstd.Zstd;
import com.limechain.runtime.hostapi.AllocatorHostFunctions;
import com.limechain.runtime.hostapi.ChildStorageHostFunctions;
import com.limechain.runtime.hostapi.CryptoHostFunctions;
import com.limechain.runtime.hostapi.HashingHostFunctions;
import com.limechain.runtime.hostapi.HostApi;
import com.limechain.runtime.hostapi.MiscellaneousHostFunctions;
import com.limechain.runtime.hostapi.OffchainHostFunctions;
import com.limechain.runtime.hostapi.StorageHostFunctions;
import com.limechain.runtime.hostapi.TrieHostFunctions;
import com.limechain.utils.ByteArrayUtils;
import lombok.extern.java.Log;
import org.wasmer.ImportObject;
import org.wasmer.Imports;
import org.wasmer.Module;

import java.util.ArrayList;
import java.util.Arrays;

@Log
public class RuntimeBuilder {
    public static final byte[] ZSTD_PREFIX = new byte[]{82, -68, 83, 118, 70, -37, -114, 5};
    public static final int MAX_ZSTD_DECOMPRESSED_SIZE = 50 * 1024 * 1024;
    public static final int DEFAULT_HEAP_PAGES = 2048;

    public static Runtime buildRuntime(byte[] code) {

        byte[] wasmBinary;
        byte[] wasmBinaryPrefix = Arrays.copyOfRange(code, 0, 8);
        if (Arrays.equals(wasmBinaryPrefix, ZSTD_PREFIX)) {
            wasmBinary = Zstd.decompress(Arrays.copyOfRange(
                    code, ZSTD_PREFIX.length, code.length), MAX_ZSTD_DECOMPRESSED_SIZE);
        } else wasmBinary = code;

        Module module = new Module(wasmBinary);
        Runtime runtime = new Runtime(module, DEFAULT_HEAP_PAGES);
        RuntimeVersion runtimeVersion = getRuntimeVersion(wasmBinary, runtime);
        runtime.setVersion(runtimeVersion);
        HostApi.setRuntime(runtime);
        return runtime;
    }

    private static RuntimeVersion getRuntimeVersion(byte[] wasmBinary, Runtime runtime) {
        // byte value of \0asm concatenated with 0x1, 0x0, 0x0, 0x0 from smoldot runtime_version.rs#97
        byte[] searchKey = new byte[]{0x00, 0x61, 0x73, 0x6D, 0x1, 0x0, 0x0, 0x0};

        int searchedKeyIndex = ByteArrayUtils.indexOf(wasmBinary, searchKey);
        if (searchedKeyIndex < 0) throw new RuntimeException("Key not found in runtime code");
        WasmSections wasmSections = new WasmSections();
        wasmSections.parseCustomSections(wasmBinary);
        if (wasmSections.getRuntimeVersion() != null && wasmSections.getRuntimeVersion().getRuntimeApis() != null) {
            return wasmSections.getRuntimeVersion();
        } else throw new RuntimeException("Could not get Runtime version");
    }

    static Imports getImports(Module module) {
        ImportObject.MemoryImport memory = new ImportObject.MemoryImport("env", 22, false);

        ArrayList<ImportObject> objects = new ArrayList<>();
        objects.addAll(StorageHostFunctions.getFunctions());
        objects.addAll(ChildStorageHostFunctions.getFunctions());
        objects.addAll(CryptoHostFunctions.getFunctions());
        objects.addAll(HashingHostFunctions.getFunctions());
        objects.addAll(OffchainHostFunctions.getFunctions());
        objects.addAll(TrieHostFunctions.getFunctions());
        objects.addAll(MiscellaneousHostFunctions.getFunctions());
        objects.addAll(AllocatorHostFunctions.getFunctions());
        objects.add(memory);

        return Imports.from(objects, module);
    }
}
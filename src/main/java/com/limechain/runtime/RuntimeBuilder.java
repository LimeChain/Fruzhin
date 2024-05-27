package com.limechain.runtime;

import com.github.luben.zstd.Zstd;
import com.limechain.exception.global.RuntimeCodeException;
import com.limechain.exception.trie.TrieDecoderException;
import com.limechain.runtime.hostapi.AllocatorHostFunctions;
import com.limechain.runtime.hostapi.ChildStorageHostFunctions;
import com.limechain.runtime.hostapi.CryptoHostFunctions;
import com.limechain.runtime.hostapi.HashingHostFunctions;
import com.limechain.runtime.hostapi.MiscellaneousHostFunctions;
import com.limechain.runtime.hostapi.OffchainHostFunctions;
import com.limechain.runtime.hostapi.StorageHostFunctions;
import com.limechain.runtime.hostapi.TrieHostFunctions;
import com.limechain.runtime.version.RuntimeVersion;
import com.limechain.runtime.version.scale.RuntimeVersionReader;
import com.limechain.trie.decoded.Trie;
import com.limechain.trie.decoded.TrieVerifier;
import com.limechain.utils.LittleEndianUtils;
import com.limechain.utils.StringUtils;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.types.Hash256;
import lombok.extern.java.Log;
import org.wasmer.ImportObject;
import org.wasmer.Module;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Level;

@Log
public class RuntimeBuilder {
    private static final byte[] ZSTD_PREFIX = new byte[]{82, -68, 83, 118, 70, -37, -114, 5};
    private static final int MAX_ZSTD_DECOMPRESSED_SIZE = 50 * 1024 * 1024;
    public static final int DEFAULT_HEAP_PAGES = 2048;
    private static final byte[] CODE_KEY_BYTES =
            LittleEndianUtils.convertBytes(StringUtils.hexToBytes(StringUtils.toHex(":code")));

    /**
     * Builds and returns a Runtime object for executing WebAssembly code.
     *
     * @param code The WebAssembly bytecode.
     * @return A Runtime object.
     */
    public Runtime buildRuntime(byte[] code) {
        byte[] wasmBinary = zstDecompressIfNecessary(code);

        Module module = new Module(wasmBinary);
        ImportObject.MemoryImport memoryImport = WasmSectionUtils.parseMemoryImportFromBinary(wasmBinary);
        Runtime runtime = new Runtime(module, memoryImport, DEFAULT_HEAP_PAGES);

        RuntimeVersion runtimeVersion = WasmSectionUtils.parseRuntimeVersionFromBinary(wasmBinary);

        //If we couldn't get the data from the wasm custom sections fallback to Core_version call
        if (runtimeVersion == null) {
            log.log(Level.INFO, "Couldn't fetch runtime version from custom section, calling 'Core_version'.");
            runtimeVersion = this.getRuntimeVersionFromRuntime(runtime);
        }

        runtime.setVersion(runtimeVersion);
        return runtime;
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

    private RuntimeVersion getRuntimeVersionFromRuntime(Runtime runtime) {
        byte[] data = runtime.call("Core_version");

        ScaleCodecReader reader = new ScaleCodecReader(data);
        RuntimeVersionReader runtimeVersionReader = new RuntimeVersionReader();
        return runtimeVersionReader.read(reader);
    }

    static ArrayList<ImportObject> getImports(ImportObject.MemoryImport memoryImport, Runtime runtime) {
        ArrayList<ImportObject> objects = new ArrayList<>();
        objects.addAll(StorageHostFunctions.getFunctions(runtime));
        objects.addAll(ChildStorageHostFunctions.getFunctions(runtime));
        objects.addAll(CryptoHostFunctions.getFunctions(runtime));
        objects.addAll(HashingHostFunctions.getFunctions(runtime));
        objects.addAll(OffchainHostFunctions.getFunctions(runtime));
        objects.addAll(TrieHostFunctions.getFunctions(runtime));
        objects.addAll(MiscellaneousHostFunctions.getFunctions(runtime));
        objects.addAll(AllocatorHostFunctions.getFunctions(runtime));
        objects.add(memoryImport);

        return objects;
    }

    /**
     * Builds and returns the runtime code based on decoded proofs and state root hash.
     *
     * @param decodedProofs The decoded trie proofs.
     * @param stateRoot     The state root hash.
     * @return The runtime code.
     * @throws RuntimeCodeException if an error occurs during the construction of the trie or retrieval of the code.
     */
    public byte[] buildRuntimeCode(byte[][] decodedProofs, Hash256 stateRoot) {
        try {
            Trie trie = TrieVerifier.buildTrie(decodedProofs, stateRoot.getBytes());
            var code = trie.get(CODE_KEY_BYTES);
            if (code == null) {
                throw new RuntimeCodeException("Couldn't retrieve runtime code from trie");
            }
            //TODO Heap pages should be fetched from out storage
            log.log(Level.INFO, "Runtime and heap pages downloaded");
            return code;

        } catch (TrieDecoderException e) {
            throw new RuntimeCodeException("Couldn't build trie from proofs list: " + e.getMessage());
        }
    }
}
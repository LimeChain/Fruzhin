package com.limechain.runtime;

import com.github.luben.zstd.Zstd;
import com.limechain.config.HostConfig;
import com.limechain.network.Network;
import com.limechain.network.protocol.blockannounce.NodeRole;
import com.limechain.rpc.server.AppBean;
import com.limechain.runtime.allocator.FreeingBumpHeapAllocator;
import com.limechain.runtime.hostapi.DefaultHostApi;
import com.limechain.runtime.hostapi.HostApi;
import com.limechain.runtime.hostapi.WasmExports;
import com.limechain.runtime.hostapi.dto.OffchainNetworkState;
import com.limechain.runtime.memory.WasmMemory;
import com.limechain.runtime.version.RuntimeVersion;
import com.limechain.storage.KVRepository;
import com.limechain.storage.crypto.KeyStore;
import com.limechain.storage.offchain.OffchainStorages;
import com.limechain.storage.offchain.OffchainStore;
import com.limechain.storage.offchain.StorageKind;
import com.limechain.trie.AccessorHolder;
import com.limechain.trie.BlockTrieAccessor;
import io.libp2p.core.Host;
import lombok.extern.java.Log;
import org.jetbrains.annotations.Nullable;
import org.wasmer.ImportObject;
import org.wasmer.Imports;
import org.wasmer.Instance;
import org.wasmer.Memory;
import org.wasmer.Module;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.logging.Level;

/**
 * A builder for runtime instances by provided bytecode source.
 * This is the public interface for constructing {@link Runtime} instances.
 */
@Log
public class RuntimeBuilder {
    private static final byte[] ZSTD_PREFIX = new byte[] {82, -68, 83, 118, 70, -37, -114, 5};
    private static final int MAX_ZSTD_DECOMPRESSED_SIZE = 50 * 1024 * 1024;

    // TODO: Do we want this implicit logic for building with the spring context to reside here (within the Builder)
    //  or delegate it somewhere else (user-site)?
    /**
     * Builds and returns a ready-to-execute `Runtime` with dependencies from the global Spring context.
     *
     * @param code the runtime wasm bytecode
     * @return a ready to execute `Runtime` instance
     * @implNote Make sure the Spring context is properly initialized, as building the runtime this way relies on it.
     */
    public Runtime buildRuntime(byte[] code) {
        var nodeRole = AppBean.getBean(HostConfig.class).getNodeRole();

        var db = AppBean.getBean(KVRepository.class);
        var localStorage = new OffchainStore(db, StorageKind.LOCAL);
        var persistentStorage = new OffchainStore(db, StorageKind.PERSISTENT);
        // TODO:
        //  This shouldn't be like that: base storage (for offchain_index api) is not the same as persistent storage.
        //  Figure out the right approach, when it becomes relevant.
        var baseStorage = persistentStorage;
        var offchainStorages = new OffchainStorages(localStorage, persistentStorage, baseStorage);

        var keyStore = AppBean.getBean(KeyStore.class);
        var blockTrieAccessor = AccessorHolder.getInstance().getBlockTrieAccessor();

        Host host = AppBean.getBean(Network.class).getHost();
        var offchainNetworkState = new OffchainNetworkState(host.getPeerId(), host.listenAddresses());

        RuntimeBuilder.Config cfg = new RuntimeBuilder.Config(
            blockTrieAccessor,
            keyStore,
            offchainStorages,
            offchainNetworkState,
            nodeRole == NodeRole.AUTHORING
        );

        return this.buildRuntime(code, cfg);
    }

    /**
     * Builds and returns a ready-to-execute `Runtime` with an explicit context configuration.
     *
     * @param code   the runtime wasm bytecode
     * @param config an explicit configuration of necessary dependencies
     * @return a ready to execute `Runtime` instance
     */
    public Runtime buildRuntime(byte[] code, Config config) {
        return this.buildRuntime(code, config, DefaultHostApi::new);
    }

    /**
     * Builds and returns a ready-to-execute `Runtime` with an explicit context configuration
     * and a provided custom Host API implementation.
     *
     * @param code            the runtime wasm bytecode
     * @param config          an explicit configuration of necessary dependencies
     * @param hostApiProvider a provider for a {@link HostApi} implementation, given the current execution {@link Context}.
     * @return a ready to execute `Runtime` instance
     * @apiNote Providing a custom implementation requires you to extend {@link HostApi}
     * with your custom implementation class.
     * The runtime execution {@link Context} is exposed to you for this purpose.
     * It is minimally contractually binding to construct a {@link HostApi},
     * but you can choose to either use it or not.
     */
    public Runtime buildRuntime(byte[] code, Config config, Function<Context, HostApi> hostApiProvider) {
        byte[] wasmBinary = zstDecompressIfNecessary(code);

        Module module = new Module(wasmBinary);

        SharedMemory sharedMemory = new SharedMemory(null, null);
        Context context = new Context(
            config.blockTrieAccessor(),
            config.keyStore(),
            config.offchainStorages(),
            config.offchainNetworkState(),
            config.isValidator(),
            sharedMemory,
            null
        );

        // Construct the host API implementation
        HostApi hostApi = hostApiProvider.apply(context);

        // Build the imports
        // TODO: Extract the memory import from the runtime, don't hardcode.
        ImportObject.MemoryImport memory = new ImportObject.MemoryImport("env", 22, false);
        List<ImportObject.FuncImport> functionImports = hostApi.getFunctionImports();

        List<ImportObject> imports = new ArrayList<>(functionImports);
        imports.add(memory);

        // Instantiate the wasm module
        Instance instance = module.instantiate(Imports.from(imports, module));

        // Construct our Runtime instance
        Runtime runtime = new Runtime(module, context, instance);

        // Inject the wasm memory and the allocator into the shared memory
        // NOTE:
        //  This injection is necessarily delayed to circumvent the circular dependency:
        //  instance needs imports -> imports need shared memory -> shared memory needs instance (to fetch the memory from)
        Memory instanceMemory = instance.exports.getMemory(WasmExports.MEMORY.getValue());
        int heapBase = instance.exports.getGlobal(WasmExports.HEAP_BASE.getValue()).getIntValue();
        sharedMemory.setMemory(new WasmMemory(instanceMemory));
        sharedMemory.setAllocator(new FreeingBumpHeapAllocator(heapBase));

        // We cache the runtime version in the context as it's often needed
        cacheRuntimeVersion(wasmBinary, runtime, context);

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

    private void cacheRuntimeVersion(byte[] wasmBinary, Runtime runtime, Context context) {
        // Attempt parsing the runtime version from custom sections
        RuntimeVersion runtimeVersion = WasmSectionUtils.parseRuntimeVersionFromCustomSections(wasmBinary);

        // If we couldn't get the data from the wasm custom sections,
        // we must fall back to calling Core_version
        if (runtimeVersion == null) {
            log.log(Level.INFO, "Couldn't fetch runtime version from custom section, calling 'Core_version'.");
            runtimeVersion = runtime.callCoreVersion();
        }

        context.setRuntimeVersion(runtimeVersion);
    }

    /**
     * A configuration holding all necessary dependencies for runtime calls.
     *
     * @param blockTrieAccessor used by storage related endpoints for accessing the trie storage for a block
     * @param keyStore          used by crypto host functions to access secret keys
     * @param offchainStorages  used by offchain host functions to access the different offchain storages
     * @param offchainNetworkState used by offchain host functions.
     * @param isValidator       used by offchain host functions to determine
     *                          whether the node is a validator (i.e. authoring node) or not
     * @apiNote Explicitly passing `null`s for some of the dependencies is admissible in case you desire a partial
     * runtime execution context (e.g. when you need a really lightweight runtime invocation).
     * The implicit knowledge of whether a dependency will be used for the runtime invocations about to be performed,
     * however, still remains a responsibility of the caller.
     */
    public record Config(
        @Nullable
        BlockTrieAccessor blockTrieAccessor,
        @Nullable
        KeyStore keyStore,
        @Nullable
        OffchainStorages offchainStorages,
        @Nullable
        OffchainNetworkState offchainNetworkState,
        boolean isValidator
    ) {
        public static final Config EMPTY = new Config(null, null, null, null, false);
    }
}
package com.limechain.runtime.hostapi;

import com.limechain.runtime.Context;
import org.wasmer.ImportObject;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * The default total implementation of the Host API.
 */
public class DefaultHostApi extends HostApi {
    public DefaultHostApi(Context context) {
        super(context);
    }

    @Override
    protected Map<Endpoint, ImportObject.FuncImport> buildFunctionImports() {
        List<PartialHostApi> impls = List.of(
            new AllocatorHostFunctions(sharedMemory),
            new HashingHostFunctions(sharedMemory),
            new StorageHostFunctions(sharedMemory, context.getBlockTrieAccessor()),
            new TrieHostFunctions(sharedMemory),
            new MiscellaneousHostFunctions(sharedMemory),
            new OffchainHostFunctions(sharedMemory, context.getOffchainStorages(), context.isValidator()),
            new CryptoHostFunctions(sharedMemory, context.getKeyStore()),
            new ChildStorageHostFunctions(sharedMemory, context.getBlockTrieAccessor())
        );

        return impls.stream()
            .flatMap(impl -> impl.getFunctionImports().entrySet().stream())
            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}

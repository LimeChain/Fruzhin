package com.limechain.runtime.hostapi;

import com.limechain.runtime.Context;
import com.limechain.runtime.SharedMemory;
import lombok.extern.java.Log;
import org.wasmer.ImportObject;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * An abstract class defining a total implementation of the Host API.
 * Crucially, it makes sure that all {@link Endpoint}s have been implemented
 * ("implemented" meaning, an {@link org.wasmer.ImportObject.FuncImport} has been provided).
 *
 * @apiNote All implementations rely on the {@link Context} for runtime executions.
 */
@Log
public abstract class HostApi {
    protected Context context;
    protected SharedMemory sharedMemory;

    protected HostApi(Context context) {
        this.context = context;
        this.sharedMemory = context.getSharedMemory();
    }

    /**
     * @return a list of import objects for all {@link Endpoint}s
     */
    public final List<ImportObject.FuncImport> getFunctionImports() {
        var imports = this.buildFunctionImports();

        // Assert all endpoints have been implemented, exactly once (because we collect in a set).
        // NOTE:
        //  We do that as an internal sanity check, but it could easily be relaxed if needed:
        //  e.g. if you want to build empty imports for missing endpoints instead of strictly throwing.
        Set<Endpoint> got = imports.keySet();

        Set<Endpoint> unimplemented = EnumSet.allOf(Endpoint.class);
        unimplemented.removeAll(got);

        assert unimplemented.isEmpty()
            : String.format("Missing imports:%n%s",
                unimplemented.stream().map(Endpoint::name).collect(Collectors.joining(System.lineSeparator())));

        return imports.values().stream().toList();
    }

    /**
     * Builds a map from all endpoints to their implementations, thus constituting the total implementation.
     * @implSpec the map is expected to contain an entry for every {@link Endpoint}
     */
    protected abstract Map<Endpoint, ImportObject.FuncImport> buildFunctionImports();
}

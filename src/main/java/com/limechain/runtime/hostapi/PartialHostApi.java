package com.limechain.runtime.hostapi;

import org.wasmer.ImportObject;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * An interface defining an implementation of a (not necessarily strict) subset of all {@link Endpoint}s.
 * @apiNote Mainly intended for partitioning a total implementation into separate classes for readability
 *          and separation of concerns.
 */
public interface PartialHostApi {
    /**
     * Builds a map from (some) endpoints to their implementations, thus constituting the partial implementation.
     * @apiNote It is left to the implementor to decide which endpoints they're going to implement.
     *          This information is encoded in the collection of keys of this map.
     */
    Map<Endpoint, ImportObject.FuncImport> getFunctionImports();

    static Map.Entry<Endpoint, ImportObject.FuncImport> newImportObjectPair(Endpoint endpoint, Function<List<Number>, Number> impl) {
        return Map.entry(endpoint, endpoint.getImportObject(impl));
    }

    static Map.Entry<Endpoint, ImportObject.FuncImport> newImportObjectPair(Endpoint endpoint, Consumer<List<Number>> impl) {
        return Map.entry(endpoint, endpoint.getImportObject(impl));
    }

    static Map.Entry<Endpoint, ImportObject.FuncImport> newImportObjectPairNotImplemented(Endpoint endpoint) {
        return Map.entry(endpoint, endpoint.getImportObjectNotImplemented());
    }
}

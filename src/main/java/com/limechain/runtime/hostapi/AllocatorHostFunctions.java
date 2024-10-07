package com.limechain.runtime.hostapi;

import com.limechain.runtime.SharedMemory;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.java.Log;
import org.wasmer.ImportObject;

import java.util.Map;

import static com.limechain.runtime.hostapi.PartialHostApi.newImportObjectPair;

/**
 * Implementations of the Allocator HostAPI functions
 * For more info check
 * {<a href="https://spec.polkadot.network/chap-host-api#sect-allocator-api">Allocator API</a>}
 */
@Log
@AllArgsConstructor(access = AccessLevel.PACKAGE)
public class AllocatorHostFunctions implements PartialHostApi {
    private final SharedMemory sharedMemory;

    @Override
    public Map<Endpoint, ImportObject.FuncImport> getFunctionImports() {
        return Map.ofEntries(
            newImportObjectPair(Endpoint.ext_allocator_malloc_version_1, argv -> {
                return extAllocatorMallocVersion1(argv.get(0).intValue());
            }),
            newImportObjectPair(Endpoint.ext_allocator_free_version_1, argv -> {
                extAllocatorFreeVersion1(argv.get(0).intValue());
            })
        );
    }

    /**
     * Allocates the given number of bytes and returns the pointer to that memory location.
     *
     * @param size the size of the buffer to be allocated.
     * @return a pointer to the allocated buffer.
     */
    public int extAllocatorMallocVersion1(int size) {
        log.finest("extAllocatorMallocVersion1");
        return sharedMemory.allocate(size).pointer();

    }

    /**
     * Free the given pointer.
     *
     * @param pointer a pointer to the memory buffer to be freed.
     */
    public void extAllocatorFreeVersion1(int pointer) {
        log.finest("extAllocatorFreeVersion1");
        sharedMemory.deallocate(pointer);
    }
}

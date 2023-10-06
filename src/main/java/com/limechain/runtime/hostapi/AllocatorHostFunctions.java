package com.limechain.runtime.hostapi;

import lombok.experimental.UtilityClass;
import org.wasmer.ImportObject;
import org.wasmer.Memory;
import org.wasmer.Type;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.List;

/**
 * Implementations of the Allocator HostAPI functions
 * For more info check
 * {<a href="https://spec.polkadot.network/chap-host-api#sect-allocator-api">Allocator API</a>}
 */
@UtilityClass
public class AllocatorHostFunctions {
    public static List<ImportObject> getFunctions() {
        return Arrays.asList(
                HostApi.getImportObject("ext_allocator_malloc_version_1", argv -> {
                    return extAllocatorMallocVersion1((int) argv.get(0));
                }, List.of(Type.I32), Type.I32),
                HostApi.getImportObject("ext_allocator_free_version_1", argv -> {
                    //TODO: Try marking the part of the bytebuffer as free?
                    //Not sure if currently we can support freeing the memory in java
                }, List.of(Type.I32)));
    }

    private static int extAllocatorMallocVersion1(int size) {
        Memory memory = HostApi.getInstance().getMemory();
        ByteBuffer buffer = HostApi.getByteBuffer(memory);
        int position = buffer.position();
        if (size > buffer.limit() - position) {
            memory.grow(buffer.limit() - position);
        }
        buffer.position(position + size);

        return position;
    }
}

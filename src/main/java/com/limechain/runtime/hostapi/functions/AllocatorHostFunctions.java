package com.limechain.runtime.hostapi.functions;

import com.limechain.runtime.hostapi.HostApi;
import lombok.experimental.UtilityClass;
import org.wasmer.ImportObject;
import org.wasmer.Type;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@UtilityClass
public class AllocatorHostFunctions {

    public static List<ImportObject> getFunctions() {
        return Arrays.asList(
                HostFunctions.getImportObject("ext_allocator_malloc_version_1", argv -> {
                    return Collections.singletonList(HostApi.extAllocatorMallocVersion1((int) argv.get(0)));
                }, List.of(Type.I32), Type.I32),
                HostFunctions.getImportObject("ext_allocator_free_version_1", argv -> {
                    //TODO: Try marking the part of the bytebuffer as free?
                    //Not sure if currently we can support freeing the memory in java
                }, List.of(Type.I32)));
    }

}

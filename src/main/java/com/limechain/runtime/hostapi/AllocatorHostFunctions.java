package com.limechain.runtime.hostapi;

import org.wasmer.ImportObject;
import org.wasmer.Type;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AllocatorHostFunctions {

    public static List<ImportObject> getFunctions() {
        return Arrays.asList(
                new ImportObject.FuncImport("env", "ext_allocator_malloc_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_allocator_malloc_version_1'");
                    return Collections.singletonList(HostApi.extAllocatorMallocVersion1((int) argv.get(0)));
                }, List.of(Type.I32), List.of(Type.I32)),
                new ImportObject.FuncImport("env", "ext_allocator_free_version_1", argv -> {
                    System.out.println("Message printed in the body of 'ext_allocator_free_version_1'");
                    HostApi.extAllocatorFreeVersion1();
                    return Collections.emptyList();
                }, List.of(Type.I32), List.of()));
    }

}

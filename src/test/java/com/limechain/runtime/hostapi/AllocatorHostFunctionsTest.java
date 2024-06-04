package com.limechain.runtime.hostapi;

import com.limechain.runtime.SharedMemory;
import com.limechain.runtime.hostapi.dto.RuntimePointerSize;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AllocatorHostFunctionsTest {
    @InjectMocks
    private AllocatorHostFunctions allocatorHostFunctions;

    @Mock
    private SharedMemory sharedMemory;

    @Test
    void extAllocatorMallocVersion1() {
        int size = 123;
        int pointer = 777;
        RuntimePointerSize runtimePointerSize = mock(RuntimePointerSize.class);
        when(sharedMemory.allocate(size)).thenReturn(runtimePointerSize);
        when(runtimePointerSize.pointer()).thenReturn(pointer);

        int result = allocatorHostFunctions.extAllocatorMallocVersion1(size);

        assertEquals(pointer, result);
    }

    @Test
    void extAllocatorFreeVersion1() {
        int pointer = 777;

        allocatorHostFunctions.extAllocatorFreeVersion1(pointer);

        verify(sharedMemory).deallocate(pointer);
    }
}
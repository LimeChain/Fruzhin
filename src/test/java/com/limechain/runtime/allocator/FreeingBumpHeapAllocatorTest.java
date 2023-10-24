package com.limechain.runtime.allocator;

import com.limechain.runtime.hostapi.dto.RuntimePointerSize;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.wasmer.Memory;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FreeingBumpHeapAllocatorTest {
    private static final int HEAP_BASE = 123;
    private static final int MEMORY_SIZE = 456;
    private static final int ORDER_VALUE = 789;
    private static final int HEADER_SIZE = 8;

    @Mock
    private Memory memory;
    @Mock
    private ByteBuffer buffer;

    @Mock
    private List<Order> orders;

    @Mock
    private Order order;

    @Mock
    private Header header;

    @Mock
    private AllocationStats stats;

    private FreeingBumpHeapAllocator freeingBumpHeapAllocator;

    @BeforeEach
    void setup() {
        when(memory.buffer()).thenReturn(buffer);
        when(buffer.limit()).thenReturn(MEMORY_SIZE);

        freeingBumpHeapAllocator =
                new FreeingBumpHeapAllocator(HEAP_BASE, orders, stats, MEMORY_SIZE, HEAP_BASE);
    }

    @Test
    void allocateWhenMemorySizeIsSmallerThanLastObservedSizeShouldThrowError() {
        ReflectionTestUtils.setField(freeingBumpHeapAllocator, "lastObservedMemorySize", MEMORY_SIZE + 1);

        assertThrows(
                AllocationError.class,
                () -> freeingBumpHeapAllocator.allocate(1, memory),
                "Memory shrank");
    }

    @Test
    void allocateWhenSizeIsTooBigShouldThrowError() {
        assertThrows(
                AllocationError.class,
                () -> freeingBumpHeapAllocator.allocate(Order.MAX_POSSIBLE_ALLOCATION + 1, memory),
                "Requested allocation size is too large");
    }

    @Test
    void allocateWhenOrderHasNextPointerShouldWriteOccupiedHeaderWithOrderAtPointer() {
        int headerPointer = 134;
        when(orders.get(anyInt())).thenReturn(order);
        when(order.getValue()).thenReturn(ORDER_VALUE);
        when(order.popFreeHeaderPointer(any())).thenReturn(Optional.of(headerPointer));

        freeingBumpHeapAllocator.allocate(10, memory);

        verify(buffer).putLong(headerPointer, Header.occupied(ORDER_VALUE).raw());
    }

    @Test
    void allocateWhenOrderHasNoNextPointerShouldWriteOccupiedHeaderWithOrderAtBumper() {
        int bumper = 777;
        ReflectionTestUtils.setField(freeingBumpHeapAllocator, "bumper", bumper);
        when(orders.get(anyInt())).thenReturn(order);
        when(order.getValue()).thenReturn(ORDER_VALUE);
        when(order.popFreeHeaderPointer(any())).thenReturn(Optional.empty());

        freeingBumpHeapAllocator.allocate(10, memory);

        verify(buffer).putLong(bumper, Header.occupied(ORDER_VALUE).raw());
    }

    @Test
    void allocateWhenOrderHasNextPointerShouldReturnRuntimePointerToBlock() {
        int headerPointer = 134;
        int size = 12;
        int orderSize = 16;
        RuntimePointerSize expected = new RuntimePointerSize(headerPointer + HEADER_SIZE, size);
        when(orders.get(anyInt())).thenReturn(order);
        when(order.getValue()).thenReturn(ORDER_VALUE);
        when(order.getBlockSize()).thenReturn(orderSize);
        when(order.popFreeHeaderPointer(any())).thenReturn(Optional.of(headerPointer));

        RuntimePointerSize result = freeingBumpHeapAllocator.allocate(size, memory);

        assertEquals(expected, result);
    }

    @Test
    void allocateWhenOrderHasNoNextPointerShouldReturnRuntimePointerToBumpedBlock() {
        int bumper = 777;
        int size = 12;
        int orderSize = 16;
        RuntimePointerSize expected = new RuntimePointerSize(bumper + HEADER_SIZE, size);
        ReflectionTestUtils.setField(freeingBumpHeapAllocator, "bumper", bumper);
        when(orders.get(anyInt())).thenReturn(order);
        when(order.getValue()).thenReturn(ORDER_VALUE);
        when(order.getBlockSize()).thenReturn(orderSize);
        when(order.popFreeHeaderPointer(any())).thenReturn(Optional.empty());

        RuntimePointerSize result = freeingBumpHeapAllocator.allocate(size, memory);

        assertEquals(expected, result);
    }

    @Test
    void allocateShouldUpdateStatsWithOrderBlockSizeAndHeader() {
        int size = 12;
        when(orders.get(anyInt())).thenReturn(order);
        when(order.getValue()).thenReturn(ORDER_VALUE);
        when(order.getBlockSize()).thenReturn(size);
        when(order.popFreeHeaderPointer(any())).thenReturn(Optional.of(1));

        freeingBumpHeapAllocator.allocate(size, memory);

        verify(stats).allocated(size + HEADER_SIZE, 0);

    }

    @Test
    void deallocateWhenMemorySizeIsSmallerThanLastObservedSizeShouldThrowError() {
        ReflectionTestUtils.setField(freeingBumpHeapAllocator, "lastObservedMemorySize", MEMORY_SIZE + 1);

        assertThrows(
                AllocationError.class,
                () -> freeingBumpHeapAllocator.deallocate(1, memory),
                "Memory shrank");
    }

    @Test
    void deallocateWhenPointerBelowHeapBaseShouldThrowError() {
        assertThrows(
                AllocationError.class,
                () -> freeingBumpHeapAllocator.deallocate(HEAP_BASE - 1, memory),
                "Invalid pointer for deallocation");
    }

    @Test
    void deallocateWhenOrderHasPointerToFreeHeaderShouldWriteFreeHeaderWithPointerBeforeDeallocatedBlock() {
        int pointer = HEAP_BASE + 123;
        int freeHeaderPointer = 542;

        try(MockedStatic<Header> mockedStatic = mockStatic(Header.class, CALLS_REAL_METHODS)) {
            mockedStatic.when(() -> Header.fromMemory(pointer - HEADER_SIZE, memory)).thenReturn(header);
            when(header.isOccupied()).thenReturn(true);
            when(header.getOrder()).thenReturn(ORDER_VALUE);
            when(orders.get(ORDER_VALUE)).thenReturn(order);
            when(order.getFreeHeaderPointer()).thenReturn(freeHeaderPointer);

            freeingBumpHeapAllocator.deallocate(pointer, memory);

            verify(buffer).putLong(pointer - HEADER_SIZE, Header.free(freeHeaderPointer).raw());
        }
    }

    @Test
    void deallocateWhenOrderHasNoPointerToFreeHeaderShouldWriteFreeHeaderWithMaxIntPointerBeforeDeallocatedBlock() {
        int pointer = HEAP_BASE + 123;

        try(MockedStatic<Header> mockedStatic = mockStatic(Header.class, CALLS_REAL_METHODS)) {
            mockedStatic.when(() -> Header.fromMemory(pointer - HEADER_SIZE, memory)).thenReturn(header);
            when(header.isOccupied()).thenReturn(true);
            when(header.getOrder()).thenReturn(ORDER_VALUE);
            when(orders.get(ORDER_VALUE)).thenReturn(order);
            when(order.getFreeHeaderPointer()).thenReturn(null);

            freeingBumpHeapAllocator.deallocate(pointer, memory);

            verify(buffer).putLong(pointer - HEADER_SIZE, Header.free(Integer.MAX_VALUE).raw());
        }
    }

    @Test
    void deallocateShouldUpdateOrderFreeHeaderPointerWithDeallocatedHeader() {
        int pointer = HEAP_BASE + 123;

        try(MockedStatic<Header> mockedStatic = mockStatic(Header.class, CALLS_REAL_METHODS)) {
            mockedStatic.when(() -> Header.fromMemory(pointer - HEADER_SIZE, memory)).thenReturn(header);
            when(header.isOccupied()).thenReturn(true);
            when(header.getOrder()).thenReturn(ORDER_VALUE);
            when(orders.get(ORDER_VALUE)).thenReturn(order);

            freeingBumpHeapAllocator.deallocate(pointer, memory);

            verify(order).setFreeHeaderPointer(pointer - HEADER_SIZE);
        }
    }

    @Test
    void deallocateShouldUpdateStats() {
        int pointer = HEAP_BASE + 123;
        int blockSize = 777;

        try(MockedStatic<Header> mockedStatic = mockStatic(Header.class, CALLS_REAL_METHODS)) {
            mockedStatic.when(() -> Header.fromMemory(pointer - HEADER_SIZE, memory)).thenReturn(header);
            when(header.isOccupied()).thenReturn(true);
            when(header.getOrder()).thenReturn(ORDER_VALUE);
            when(orders.get(ORDER_VALUE)).thenReturn(order);
            when(order.getBlockSize()).thenReturn(blockSize);

            freeingBumpHeapAllocator.deallocate(pointer, memory);

            verify(stats).deallocated(blockSize);
        }
    }
}
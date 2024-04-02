package com.limechain.runtime.allocator;

import com.limechain.runtime.hostapi.dto.RuntimePointerSize;
import lombok.AllArgsConstructor;
import lombok.extern.java.Log;
import org.wasmer.Memory;

import java.util.ArrayList;
import java.util.List;

import static com.limechain.runtime.allocator.Order.MAX_POSSIBLE_ALLOCATION;
import static com.limechain.runtime.allocator.Order.MIN_POSSIBLE_ALLOCATION;
import static com.limechain.runtime.allocator.Order.NUMBER_OF_ORDERS;

/**
 * <b>Freeing-bump allocator implementation.</b>
 * <br>
 * The heap is a continuous linear memory and chunks are allocated using a bump allocator.
 * <br>
 * Only allocations with sizes of power of two can be allocated. If the incoming request has a non-power
 * of two size it is increased to the nearest power of two. The power of two of size is
 * referred as an {@link Order}.
 * <br>
 * Each allocation has a {@link Header} immediately preceding it. The header is always 8 bytes and can
 * be either free or occupied.
 * <br>
 * For implementing freeing we maintain a linked lists for each order, by each order pointing to the
 * {@link Order#getFreeHeaderPointer() next free header}
 * and each free header pointing to the {@link Header#getNextFreeHeaderPointer() next free one}.
 * <br>
 * The maximum supported allocation size is capped, therefore the number of orders and thus the linked lists is as well
 * limited. Currently, the maximum size of an allocation is 32 MiB.
 * <br>
 * When the allocator serves an allocation request it first checks the linked list for the
 * respective order. If it doesn't have any free chunks, the allocator requests memory from the
 * bump allocator. In any case the order is stored in the header of the allocation.
 * <br>
 * Upon deallocation, we get the order of the allocation from its header and then add that
 * allocation to the linked list for the respective order.
 * <br><br>
 * <b>Caveats</b>
 * <br>
 * This is a fast allocator, but it is also dumb. There are specifically two main shortcomings
 * that the user should keep in mind:
 * <ul>
 * <li>
 *   Once the bump allocator space is exhausted, there is no way to reclaim the memory. This means
 *   that it's possible to end up in a situation where there are no live allocations yet a new
 *   allocation will fail.
 *   <br>
 *   Let's look into an example. Given a heap of 32 MiB. The user makes a 32 MiB allocation that we
 *   call `X` . Now the heap is full. Then user deallocates `X`. Since all the space in the bump
 *   allocator was consumed by the 32 MiB allocation, allocations of all sizes except 32 MiB will
 *   fail.
 * </li>
 * <li>
 *   Sizes of allocations are rounded up to the nearest order. That is, an allocation of 2,00001
 *   MiB will be put into the bucket of 4 MiB. Therefore, any allocation of size `(N, 2N]` will
 *   take up to `2N`, thus assuming a uniform distribution of allocation sizes, the average amount
 *   in use of a `2N` space on the heap will be `(3N + ε) / 2`. So average utilization is going to
 *   be around 75% (`(3N + ε) / 2 / 2N`) meaning that around 25% of the space in allocation will be
 *   wasted. This is more pronounced (in terms of absolute heap amounts) with larger allocation
 *   sizes.
 * </li>
 * </ul>
 */
@Log
@AllArgsConstructor
public class FreeingBumpHeapAllocator {
    private static final int ALIGNMENT = 8;
    private static final int HEADER_SIZE = 8;
    private static final int PAGE_SIZE = 65536;
    private static final int MAX_WASM_PAGES = (int) (4L * 1024 * 1024 * 1024 / PAGE_SIZE); // 4GB

    private final int originalHeapBase;
    private final List<Order> orders;
    private final AllocationStats stats;
    private int lastObservedMemorySize;
    private int bumper;

    public FreeingBumpHeapAllocator(int heapBase) {
        int alignedHeapBase = (heapBase + ALIGNMENT - 1) / ALIGNMENT * ALIGNMENT;

        this.originalHeapBase = alignedHeapBase;
        this.bumper = alignedHeapBase;
        this.orders = new ArrayList<>(NUMBER_OF_ORDERS);
        this.lastObservedMemorySize = 0;
        this.stats = new AllocationStats();

        for (int i = 0; i < NUMBER_OF_ORDERS; i++) {
            orders.add(new Order(i));
        }
    }

    /**
     * Allocates a block for given size in memory.
     * <br>The block allocated will have size equal to the next power of 2, relative to the given size.
     * <br>The block will be allocated at the next free header pointed by the {@link Order order} of this size, if any.
     * Otherwise, memory will be bumped and the new space will be allocated.
     * <br>An occupied {@link Header header} is written before the allocated block.
     *
     * @param size   size to be allocated; max size is 32MB
     * @param memory memory
     * @return pointer-size to the allocated memory
     * @throws AllocationError when a block of this size can't be allocated
     */
    public RuntimePointerSize allocate(int size, Memory memory) {
        log.finer("++HF++ Allocating memory... size=" + size);
        verifyMemorySize(memory);
        Order order = getOrderForSize(size);
        int headerPointer = nextFreeHeaderPointer(order, memory);
        writeOccupiedHeader(headerPointer, order.getValue(), memory);
        stats.allocated(order.getBlockSize() + HEADER_SIZE, bumper - originalHeapBase);
        log.finer("++HF++ Allocated " + stats.getBytesAllocated() + " bytes successfully");
        log.finer("++HF++ Total Allocated Memory: " + stats.getBytesAllocatedSum().toString());
        return new RuntimePointerSize(headerPointer + HEADER_SIZE, size);

    }

    private Order getOrderForSize(int size) {
        if (size > MAX_POSSIBLE_ALLOCATION) {
            throw new AllocationError("Requested allocation size is too large");
        }
        if (size <= MIN_POSSIBLE_ALLOCATION) {
            return orders.get(0);
        }

        int order = nextPowerOfTwo(size) - nextPowerOfTwo(MIN_POSSIBLE_ALLOCATION);
        return orders.get(order);
    }

    private int nextPowerOfTwo(int value) {
        return Integer.SIZE - Integer.numberOfLeadingZeros(value - 1);
    }

    private int nextFreeHeaderPointer(Order order, Memory memory) {
        return order.popFreeHeaderPointer(memory)
                .orElseGet(() -> bump(order.getBlockSize() + HEADER_SIZE, memory));
    }

    private int bump(int size, Memory memory) {
        long requiredSize = (long) bumper + size;

        if (requiredSize > memory.buffer().limit()) {
            growPages(size, memory);
        }

        int pointer = bumper;
        bumper += size;
        return pointer;
    }

    private void growPages(int size, Memory memory) {
        int requiredPages = pagesFromSize(size);
        int currentPages = pagesFromSize(memory.buffer().limit());

        if (currentPages >= MAX_WASM_PAGES) {
            throw new AllocationError("Max pages already reached.");
        }

        if (requiredPages > MAX_WASM_PAGES) {
            throw new AllocationError(String.format(
                    "Failed to grow memory from %d pages to at least %d pages due to the maximum limit of %d pages",
                    currentPages, requiredPages, MAX_WASM_PAGES)
            );
        }

        int nextPages = Math.min(currentPages * 2, MAX_WASM_PAGES);
        nextPages = Math.max(nextPages, requiredPages);

        memory.grow(nextPages - currentPages);
    }

    private int pagesFromSize(long size) {
        long pages = (size + PAGE_SIZE - 1) / PAGE_SIZE;
        if (pages > Integer.MAX_VALUE) {
            throw new AllocationError("Allocator ran out of space");
        }
        return (int) pages;
    }

    private void writeOccupiedHeader(int pointer, int order, Memory memory) {
        Header header = Header.occupied(order);
        memory.buffer().putLong(pointer, header.raw());
    }

    /**
     * Deallocate a block of memory.
     * <br> The header of the deallocated block is set to free with a pointer to the current
     * {@link Order#getFreeHeaderPointer() free header pointer} of its order.
     * The pointer of the order is set to the deallocated header.
     *
     * @param pointer pointer to the block
     * @param memory  memory
     * @throws AllocationError when the memory cannot be deallocated
     */
    public void deallocate(int pointer, Memory memory) {
        verifyMemorySize(memory);
        int headerPointer = pointer - HEADER_SIZE;
        Order order = getOrderFromHeader(headerPointer, memory);
        writeFreeHeader(headerPointer, order.getFreeHeaderPointer(), memory);
        order.setFreeHeaderPointer(headerPointer);
        stats.deallocated(order.getBlockSize());
    }

    private Order getOrderFromHeader(int headerPointer, Memory memory) {
        if (headerPointer < originalHeapBase) {
            throw new AllocationError("Invalid pointer for deallocation");
        }
        Header header = Header.fromMemory(headerPointer, memory);
        if (!header.isOccupied() || header.getOrder() == null) {
            throw new AllocationError("No occupied header found at address");
        }

        return orders.get(header.getOrder());
    }

    private void verifyMemorySize(Memory memory) throws AllocationError {
        int memorySize = memory.buffer().limit();
        if (memorySize < lastObservedMemorySize) {
            throw new AllocationError("Memory shrank");
        }
        lastObservedMemorySize = memorySize;
    }

    private void writeFreeHeader(int pointer, Integer nextFreeHeaderPointer, Memory memory) {
        Header header = Header.free(nextFreeHeaderPointer == null ? Integer.MAX_VALUE : nextFreeHeaderPointer);
        memory.buffer().putLong(pointer, header.raw());
    }
}

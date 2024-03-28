package com.limechain.runtime.allocator;

import lombok.Data;
import lombok.extern.java.Log;
import org.wasmer.Memory;

import java.util.Optional;
import java.util.logging.Level;

/**
 * <b>Order for blocks of certain size.</b>
 * <br> The block size of an order is a power of 2, relative to the order value.
 * <br> Orders with value 0 have size {@value MIN_POSSIBLE_ALLOCATION} bytes, with each consecutive value
 * corresponding to a size equal to the next power of 2, up to a size of {@value MAX_POSSIBLE_ALLOCATION} bytes.
 * (orders with value 1 have size 16 bytes; with value 2 - 32 bytes, etc.)
 * <br> An order includes a pointer to the next free header of this order, if any.
 */
@Log
@Data
public class Order {
    // This number corresponds to the number of powers between the minimum possible allocation and
    // maximum possible allocation, or: 2^3...2^25 (both ends inclusive, hence 23).
    public static final int NUMBER_OF_ORDERS = 23;

    public static final int MIN_POSSIBLE_ALLOCATION = 8; // 2^3 bytes, 8 bytes
    public static final int MAX_POSSIBLE_ALLOCATION = 32 * 1024 * 1024; // 2^25 bytes, 32 MiB

    private final int value;
    private final int blockSize;
    /**
     * Pointer to the header of the next free block of this order.
     */
    private Integer freeHeaderPointer;

    /**
     * Create an order with given value, calculating the block size of the order.
     *
     * @param value order
     */
    public Order(int value) {
        this.value = value;
        this.blockSize = MIN_POSSIBLE_ALLOCATION << value;
    }

    /**
     * Pop the pointer to the next free header for this order. Replace its value with the one written
     * in the {@link Header} being pointed at.
     *
     * @param memory memory to read from
     * @return Optional of the popped value or empty if there was no value.
     */
    public Optional<Integer> popFreeHeaderPointer(Memory memory) {
        if (freeHeaderPointer == null || freeHeaderPointer == Integer.MAX_VALUE) {
            return Optional.empty();
        }

        int result = freeHeaderPointer;
        freeHeaderPointer = Header.fromMemory(freeHeaderPointer, memory).getNextFreeHeaderPointer();
        log.log(Level.INFO, "Next free header pointer=" + freeHeaderPointer);
        return Optional.of(result);
    }
}

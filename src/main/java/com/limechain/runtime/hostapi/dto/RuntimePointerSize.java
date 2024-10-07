package com.limechain.runtime.hostapi.dto;

/**
 * The Runtime pointer-size type is an unsigned 64-bit integer representing two consecutive integers.
 * The least significant is Runtime pointer. The most significant provides the size of the data in bytes.
 * This representation is the primary way to exchange data of arbitrary/dynamic sizes
 * between the Runtime and the Polkadot Host.
 *
 * @param pointerSize a 64-bit long representation
 */
public record RuntimePointerSize(long pointerSize) {
    private static final long LEAST_SIGNIFICANT_BITS_MASK = 0xFFFFFFFFL;

    /**
     * Constructor combining size as the most significant bits of the pointerSize
     * and pointer as the least significant ones.
     *
     * @param pointer 32-bit integer representing a pointer to data in memory
     * @param size 32-bit representation of the size of the data in memory
     */
    public RuntimePointerSize(int pointer, int size) {
        this((long)size << 32 | (pointer & LEAST_SIGNIFICANT_BITS_MASK));
    }

    /**
     * Constructor using a Number representation of the 64-bit pointer size
     *
     * @param pointerSize a 64-bit Number representation
     */
    public RuntimePointerSize(Number pointerSize) {
        this(pointerSize.longValue());
    }

    /**
     * The Runtime pointer, representing a pointer to data in memory, represented by the least significant 32-bits.
     *
     * @return the least significant 32 bits of the pointer size
     */
    public int pointer() {
        return (int) this.pointerSize;
    }

    /**
     * The size of the data at the pointed address as an integer, represented by the most significant 32 bits
     *
     * @return the most significant 32 bits of the pointer size
     */
    public int size() {
        return (int) (pointerSize >> 32);
    }
}

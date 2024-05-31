package com.limechain.runtime.memory;

import java.nio.ByteBuffer;

/**
 * A simple minimalistic interface for a {@link Memory}.
 */
public interface Memory {
    /**
     * Exposes the underlying buffer for mutation.
     * @return the underlying {@link ByteBuffer}
     */
    ByteBuffer buffer();

    /**
     * Grows the memory by a given amount of pages.
     * @param numPages the number of pages to grow by
     * @return idk
     */
    int grow(int numPages);
}

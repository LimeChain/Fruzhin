package com.limechain.runtime.research.hybrid.memory;

import java.nio.ByteBuffer;

public interface Memory {
    ByteBuffer buffer();
    int grow(int page);
}

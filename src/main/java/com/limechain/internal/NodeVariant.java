package com.limechain.internal;

public enum NodeVariant {
    LEAF(0x40),
    BRANCH(0x80),
    BRANCH_WITH_VALUE(0xc0);

    public final int bits;
    public final int mask;

    NodeVariant(int bits) {
        this.bits = bits;
        this.mask = 0x3F;
    }
}

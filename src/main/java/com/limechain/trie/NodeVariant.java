package com.limechain.trie;

/**
 * Enum containing the different node variant bits and masks
 */
public enum NodeVariant {
    LEAF(64),
    BRANCH(128),
    BRANCH_WITH_VALUE(192);

    public final int bits;
    public final int mask;

    NodeVariant(int bits) {
        this.bits = bits;
        this.mask = 0x3F;
    }
}

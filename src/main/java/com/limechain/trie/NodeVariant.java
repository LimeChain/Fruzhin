package com.limechain.trie;

/**
 * Enum containing the different node variant bits and masks specified
 * <a href="https://spec.polkadot.network/#defn-node-header">here</a>
 */
public enum NodeVariant {
    LEAF(0b0100_0000, 0b1100_0000),
    BRANCH(0b1000_0000, 0b1100_0000),
    BRANCH_WITH_VALUE(0b1100_0000, 0b1100_0000),
    LEAF_WITH_HASHED_VALUE(0b0010_0000, 0b1110_0000),
    BRANCH_WITH_HASHED_VALUE(0b0001_0000, 0b1111_0000),
    EMPTY(0b0000_0000, 0b1111_1111),
    COMPACT_ENCODING(0b0000_0001, 0b1111_1111);
    public final int bits;
    public final int mask;

    NodeVariant(int bits, int mask) {
        this.bits = bits;
        this.mask = mask;
    }

    public int getPartialKeyLengthHeaderMask() {
        return this.mask ^ 0xFF;
    }
}

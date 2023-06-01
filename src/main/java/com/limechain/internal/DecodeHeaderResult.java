package com.limechain.internal;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DecodeHeaderResult {
    public static final int[][] variantsOrderedByBitMask = new int[][]{
            // bits, mask, variant
            new int[]{0b0100_0000, 0b1100_0000, 1}, // leaf 01
            new int[]{0b1000_0000, 0b1100_0000, 2}, // branch 10
            new int[]{0b1100_0000, 0b1100_0000, 3}, // branch 11
    };
    public byte variantBits;
    public int partialKeyLengthHeader;
    public byte partialKeyLengthHeaderMask;

    public DecodeHeaderResult(byte variantBits, int partialKeyLengthHeader, byte partialKeyLengthHeaderMask) {
        this.variantBits = variantBits;
        this.partialKeyLengthHeader = partialKeyLengthHeader;
        this.partialKeyLengthHeaderMask = partialKeyLengthHeaderMask;
    }

    public static DecodeHeaderResult decodeHeaderByte(byte header) {
        for (int i = variantsOrderedByBitMask.length - 1; i >= 0; i--) {
            byte variantBits = (byte) (header & variantsOrderedByBitMask[i][1]);
            if (variantBits != variantsOrderedByBitMask[i][0]) {
                continue;
            }

            byte partialKeyLengthHeaderMask = (byte) ~variantsOrderedByBitMask[i][1];
            byte partialKeyLengthHeader = (byte) (header & partialKeyLengthHeaderMask);
            return new DecodeHeaderResult(variantBits, partialKeyLengthHeader, partialKeyLengthHeaderMask);
        }
        throw new IllegalStateException("ErrorVariantUnknown: for header byte " + String.format("%08d", Integer.parseInt(Integer.toBinaryString(header & 0xFF))));
    }
}

package com.limechain.trie.structure.nibble;

import com.limechain.trie.structure.nibble.exceptions.NibbleFromHexDigitException;
import com.limechain.trie.structure.nibble.exceptions.NibbleFromIntegerException;

public record Nibble(byte value) {
    public static final Nibble ZERO = Nibble.fromInt(0);
    public static final Nibble MAX = Nibble.fromInt(15);

    private static final int HEX_RADIX = 16;

    /**
     * This is effectively the same constructor as {@link #fromInt(int) fromInt}.
     * Exists just for explicitness's sake, because explicit is better than implicit.
     * @param value byte representation of the nibble
     * @return the constructed Nibble
     */
    public static Nibble fromByte(byte value) {
        return Nibble.fromInt(value);
    }

    /**
     * Constructs a nibble from an int representation
     * @param value int representation of the nibble
     * @return the constructed Nibble
     * @throws NibbleFromIntegerException if the given integer is < 0 or >= 16, thus an invalid nibble
     */
    public static Nibble fromInt(int value) {
        if (value < 0) {
            throw NibbleFromIntegerException.valueNegative(value);
        }

        if (value >= HEX_RADIX) {
            throw NibbleFromIntegerException.valueTooLarge(value);
        }

        return new Nibble((byte) value);
    }

    /**
     * Constructs a nibble from a hexadecimal char representation
     * @param c the hexadecimal char representation of the nibble
     * @return the constructed Nibble
     * @throws NibbleFromHexDigitException if the given char is not a valid hexadecimal digit (i.e. nibble)
     */
    public static Nibble fromAsciiHexDigit(char c) {
        int value = Character.digit(c, HEX_RADIX);

        if (value == -1) {
            throw NibbleFromHexDigitException.invalidHexDigit(c);
        }

        return Nibble.fromInt(value);
    }

    public byte toByte() {
        return this.value;
    }

    public int toInt() {
        return this.value;
    }

    public char toLowerHexDigit() {
        return Character.forDigit(this.value, HEX_RADIX);
    }

    public char toUpperHexDigit() {
        return Character.toUpperCase(this.toLowerHexDigit());
    }

    // NOTE: Currently exists for debugging purposes only
    @Override
    public String toString() {
        return "Nibble{" +
               "value=" + value +
               '}';
    }
}

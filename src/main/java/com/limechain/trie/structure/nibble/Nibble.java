package com.limechain.trie.structure.nibble;

public class Nibble {
    public static final Nibble ZERO = Nibble.fromInt(0);
    public static final Nibble MAX = Nibble.fromInt(15);

    private static final int HEX_RADIX = 16;

    private byte value;

    private Nibble(byte value) {
        this.value = value;
    }

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
     * @throws Exception.NibbleFromIntegerException if the given integer is < 0 or >= 16, thus an invalid nibble
     */
    public static Nibble fromInt(int value) {
        if (value < 0) {
            throw Exception.NibbleFromIntegerException.NEGATIVE_VALUE;
        }

        if (value >= HEX_RADIX) {
            throw Exception.NibbleFromIntegerException.TOO_LARGE;
        }

        return new Nibble((byte) value);
    }


    /**
     * Constructs a nibble from a hexadecimal char representation
     * @param c the hexadecimal char representation of the nibble
     * @return the constructed Nibble
     * @throws Exception.NibbleFromHexDigitException if the given char is not a valid hexadecimal digit (i.e. nibble)
     */
    public static Nibble fromAsciiHexDigit(char c) {
        int value = Character.digit(c, HEX_RADIX);

        if (value == -1) {
            throw Exception.NibbleFromHexDigitException.invalidHexDigit(c);
        }

        return Nibble.fromInt(value);
    }

    public byte toByte() {
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

    public abstract static class Exception {
        private static final String VALUE_TOO_LARGE_MSG = "Value is too large (>= 16) to fit into a nibble.";
        private static final String VALUE_NEGATIVE_MSG = "Integer value is negative, can't fit into a nibble.";

        public static class NibbleFromHexDigitException extends RuntimeException {
            public static NibbleFromHexDigitException invalidHexDigit(char c) {
                return new NibbleFromHexDigitException(
                    String.format("Invalid hexadecimal digit character '%c' given to construct a Nibble.", c));
            }

            public NibbleFromHexDigitException(String message) {
                super(message);
            }
        }

        public static class NibbleFromIntegerException extends RuntimeException {
            public static final NibbleFromIntegerException NEGATIVE_VALUE =
                new NibbleFromIntegerException(VALUE_NEGATIVE_MSG);

            public static final NibbleFromIntegerException TOO_LARGE =
                new NibbleFromIntegerException(VALUE_TOO_LARGE_MSG);

            public NibbleFromIntegerException(String message) {
                super(message);
            }
        }
    }
}

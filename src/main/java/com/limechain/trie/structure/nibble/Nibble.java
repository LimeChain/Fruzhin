package com.limechain.trie.structure.nibble;

import com.limechain.trie.structure.nibble.exceptions.NibbleFromHexDigitException;
import com.limechain.trie.structure.nibble.exceptions.NibbleFromIntegerException;

import java.io.Serializable;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;

// NOTE:
//  We could consider turning Nibble into an enum
//  if wrapping a lot of half-bytes becomes too expensive
//  in terms of memory usage and referential overcrowding.
/**
 * Represents half a byte, i.e. a nibble, i.e. a four-bit sequence.
 * Immutable by design.
 */
public class Nibble implements Serializable {
    private final byte value;
    private Nibble(final int value) {
        this((byte) value);
    }
    private Nibble(final byte value) {
        this.value = value;
    }

    private static final int HEX_RADIX = 16;

    /**
     * The minimal possible Nibble, i.e. '0'
     */
    public static final Nibble ZERO = Nibble.fromInt(0);

    /**
     * The maximal possible Nibble, i.e. 'f'
     */
    public static final Nibble MAX = Nibble.fromInt(15);

    /**
     * @return a stream of all nibbles, i.e. [0, f]
     */
    public static Stream<Nibble> all() {
        return IntStream.range(0, HEX_RADIX).mapToObj(Nibble::fromInt);
    }

    /**
     * This is effectively the same constructor as {@link Nibble#fromInt(int) fromInt}.
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

        return new Nibble(value);
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

    /**
     * @return the byte representation of the nibble
     */
    public byte asByte() {
        return this.value;
    }

    /**
     * @return the int representation of the nibble
     */
    public int asInt() {
        return this.value;
    }

    /**
     * @return the lower hex char representation of the nibble
     */
    public char asLowerHexDigit() {
        return Character.forDigit(this.value, HEX_RADIX);
    }

    /**
     * @return the upper hex char representation of the nibble
     */
    public char asUpperHexDigit() {
        return Character.toUpperCase(this.asLowerHexDigit());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Nibble nibble = (Nibble) o;
        return value == nibble.value;
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    // NOTE: Currently exists for debugging purposes only
    @Override
    public String toString() {
        return String.valueOf(this.asLowerHexDigit());
    }
}

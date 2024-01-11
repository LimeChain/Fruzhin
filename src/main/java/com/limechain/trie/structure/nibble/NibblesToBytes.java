package com.limechain.trie.structure.nibble;

import java.util.ArrayList;
import java.util.List;

public class NibblesToBytes {
    /**
     * Turns a collection of nibbles into an iterator of bytes.
     * If the number of nibbles is odd, adds a `0` nibble at the beginning.
     */
    public static List<Byte> paddingPrepend(final Nibbles nibbles) {
        Nibbles prependedNibbles;

        if (nibbles.size() % 2 == 1) {
            // NOTE: Inefficient copying, could be bettered
            prependedNibbles = nibbles.add(0, Nibble.ZERO);
        } else {
            prependedNibbles = nibbles;
        }

        return convert(prependedNibbles);
    }

    /**
     * Turns a collection of nibbles into an iterator of bytes.
     * If the number of nibbles is odd, adds a `0` nibble at the end.
     */
    public static List<Byte> paddingAppend(final Nibbles nibbles) {
        Nibbles prependedNibbles;
        if (nibbles.size() % 2 == 1) {
            // NOTE: Inefficient copying, could be bettered
            prependedNibbles = nibbles.add(Nibble.ZERO);
        } else {
            prependedNibbles = nibbles;
        }

        return convert(prependedNibbles);
    }

    /**
     * Actually constructs the new list;
     * the Nibble references have been read from and the new Bytes have been constructed.
     */
    private static List<Byte> convert(Nibbles nibbles) {
        assert nibbles.size() % 2 == 0 : "Only an even number of nibbles can be converted to bytes.";

        int halfLen = nibbles.size() / 2;

        List<Byte> result = new ArrayList<>(halfLen);

        for (int i = 0; i < halfLen; ++i) {
            Nibble n1 = nibbles.get(2 * i);
            Nibble n2 = nibbles.get(2 * i + 1);
            byte b = (byte) ((n1.asInt() << 4) + n2.asInt());
            result.add(b);
        }

        return result;
    }
}

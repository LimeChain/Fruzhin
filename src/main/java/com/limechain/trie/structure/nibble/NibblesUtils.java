package com.limechain.trie.structure.nibble;

import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.StreamSupport;

@UtilityClass
public class NibblesUtils {
    /**
     * Joins a collection of nibbles into a list of bytes by
     * joining each two consecutive nibbles into a single byte.
     * If the number of nibbles is odd, adds a `0` nibble at the beginning.
     */
    public List<Byte> toBytesPrepending(final Nibbles nibbles) {
        Nibbles prependedNibbles;

        if (nibbles.size() % 2 == 1) {
            // NOTE: Inefficient copying, could be bettered
            prependedNibbles = nibbles.add(0, Nibble.ZERO);
        } else {
            prependedNibbles = nibbles;
        }

        return toBytes(prependedNibbles);
    }

    /**
     * Joins a collection of nibbles into a list of bytes by
     * joining each two consecutive nibbles into a single byte.
     * If the number of nibbles is odd, adds a `0` nibble at the end.
     */
    public List<Byte> toBytesAppending(final Nibbles nibbles) {
        Nibbles prependedNibbles;
        if (nibbles.size() % 2 == 1) {
            // NOTE: Inefficient copying, could be bettered
            prependedNibbles = nibbles.add(Nibble.ZERO);
        } else {
            prependedNibbles = nibbles;
        }

        return toBytes(prependedNibbles);
    }

    /**
     * Actually constructs the new list;
     * the Nibble references have been read from and the new Bytes have been constructed.
     */
    private List<Byte> toBytes(Nibbles nibbles) {
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

    String toLowerHexString(Iterable<Nibble> nibbles) {
        return StreamSupport.stream(nibbles.spliterator(), false)
            .map(Nibble::asLowerHexDigit)
            .collect(
                Collector.of(
                    StringBuilder::new,
                    StringBuilder::append,
                    StringBuilder::append,
                    StringBuilder::toString
                )
            );
    }
}

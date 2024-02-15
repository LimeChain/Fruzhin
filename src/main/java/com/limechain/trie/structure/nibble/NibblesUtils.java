package com.limechain.trie.structure.nibble;

import lombok.experimental.UtilityClass;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collector;
import java.util.stream.StreamSupport;

// TODO: Add unit tests
@UtilityClass
public class NibblesUtils {
    /**
     * Joins a collection of nibbles into a String by
     * joining each two consecutive nibbles into a single byte.
     * If the number of nibbles is odd, adds a `0` nibble at the beginning.
     */
    public String toStringPrepending(final Nibbles nibbles) {
        List<Byte> byteList = toBytesPrepending(nibbles);
        byte[] bytes = new byte[byteList.size()];
        for (int i = 0; i < byteList.size(); i++) {
            bytes[i] = byteList.get(i);
        }
        return new String(bytes);
    }

    /**
     * Joins a collection of nibbles into a list of bytes by
     * joining each two consecutive nibbles into a single byte.
     * If the number of nibbles is odd, adds a `0` nibble at the beginning.
     */
    public List<Byte> toBytesPrepending(final Nibbles nibbles) {
        return toBytes(nibbles, ConversionStrategy.PREPEND);
    }

    /**
     * Joins a collection of nibbles into a list of bytes by
     * joining each two consecutive nibbles into a single byte.
     * If the number of nibbles is odd, adds a `0` nibble at the end.
     */
    public List<Byte> toBytesAppending(final Nibbles nibbles) {
        return toBytes(nibbles, ConversionStrategy.APPEND);
    }

    private List<Byte> toBytes(final Nibbles nibbles, ConversionStrategy strategy) {
        List<Byte> result = new ArrayList<>((nibbles.size() + 1) / 2);
        int startFrom = 0;

        // if we want to PREPEND a zero nibble in the case of an odd number of nibbles
        if (strategy == ConversionStrategy.PREPEND && nibbles.size() % 2 != 0) {
            byte prependedNibble = nibblesToByte(Nibble.ZERO, nibbles.get(0));
            result.add(prependedNibble);
            startFrom = 1;
        }

        for (int i = startFrom; i < nibbles.size() - 1; i += 2) {
            byte decodedNibble = nibblesToByte(nibbles.get(i), nibbles.get(i+1));
            result.add(decodedNibble);
        }

        // if, instead, we want to APPEND a zero nibble in the case of an odd number of nibbles
        if (strategy == ConversionStrategy.APPEND && nibbles.size() % 2 != 0) {
            byte appendedNibble = nibblesToByte(nibbles.get(nibbles.size() - 1), Nibble.ZERO);
            result.add(appendedNibble);
        }

        return result;
    }

    private byte nibblesToByte(Nibble first, Nibble second) {
        return (byte) (first.asInt() << 4 | second.asInt());
    }

    private enum ConversionStrategy {
        APPEND, PREPEND
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

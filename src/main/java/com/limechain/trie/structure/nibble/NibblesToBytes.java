package com.limechain.trie.structure.nibble;

import java.util.ArrayList;
import java.util.List;

// TODO: This could potentially be made similar to BytesToNibbles, consider whether a simple iterator strategy would be better.
public class NibblesToBytes {
    private final Nibbles nibbles;

    /**
     * Doesn't clone the given `Nibbles`. Only serves as an iterable to provide a way to look at the existing nibbles in a new way.
     */
    public NibblesToBytes(Nibbles nibbles) {
        this.nibbles = nibbles;
    }

    /**
     * Actually constructs the new list; the Nibble references have been read from and the new Bytes have been constructed.
     */
    private List<Byte> convert(List<Nibble> nibbles) {
        assert nibbles.size() % 2 == 0 : "Only an even number of nibbles can be converted to bytes.";

        int halfLen = nibbles.size() / 2;

        List<Byte> result = new ArrayList<>(halfLen);

        for (int i = 0; i < halfLen; ++i) {
            Nibble n1 = nibbles.get(2 * i);
            Nibble n2 = nibbles.get(2 * i + 1);
            byte b = (byte) ((n1.toByte() << 4) + n2.toByte());
            result.add(b);
        }

        return result;
    }

    /**
     * Turns an iterator of nibbles into an iterator of bytes.
     * If the number of nibbles is odd, adds a `0` nibble at the beginning.
     */
    public List<Byte> paddingPrepend() {
        Nibbles prependedNibbles;
        if (this.nibbles.size() % 2 == 1) {
            // TODO: Inefficient copying, figure out a better way
            prependedNibbles = new Nibbles(this.nibbles);
            prependedNibbles.add(0, Nibble.ZERO);
        } else {
            prependedNibbles = this.nibbles;
        }

        return convert(prependedNibbles);
    }


    /**
     * Turns an iterator of nibbles into an iterator of bytes.
     * If the number of nibbles is odd, adds a `0` nibble at the end.
     */
    public List<Byte> paddingAppend() {
        Nibbles prependedNibbles;
        if (this.nibbles.size() % 2 == 1) {
            // TODO: Inefficient copying, figure out a better way
            prependedNibbles = new Nibbles(this.nibbles);
            prependedNibbles.add(Nibble.ZERO);
        } else {
            prependedNibbles = this.nibbles;
        }

        return convert(prependedNibbles);
    }
}

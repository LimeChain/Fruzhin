package com.limechain.trie.structure.nibble;

import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;

// TODO:
//  Maybe extract into an interface with a single implementor?
//  Since NibblesToBytes would have different implementations depending on how we pad the nibbles
//  But the semantics of `BytesToNibbles` is only one possible: split the bytes in the middle and iterate :D
public class BytesToNibbles implements Iterable<Nibble> {

    private final List<Byte> bytes;

    @NotNull
    @Override
    public Iterator<Nibble> iterator() {
        return new InnerIterator(bytes.iterator());
    }

    public BytesToNibbles(List<Byte> bytes) {
        this.bytes = Collections.unmodifiableList(bytes);
    }

    public BytesToNibbles(byte[] bytes) {
        this(Arrays.asList(ArrayUtils.toObject(bytes)));
    }

    private static class InnerIterator implements Iterator<Nibble> {
        private final Iterator<Byte> byteIterator;

        @Nullable
        private Nibble remainingNibble = null;

        protected InnerIterator(Iterator<Byte> byteIterator) {
            this.byteIterator = byteIterator;
        }

        @Override
        public boolean hasNext() {
            return Objects.nonNull(remainingNibble) || byteIterator.hasNext();
        }

        @Override
        public Nibble next(){
            if (!hasNext()) {
                throw new NoSuchElementException("No more nibbles to iterate.");
            }

            if (Objects.nonNull(this.remainingNibble)) {
                Nibble nibble = this.remainingNibble;
                this.remainingNibble = null;
                return nibble;
            }

            byte nextByte = byteIterator.next();

            Nibble leftNibble = Nibble.fromByte((byte) ((nextByte & 0xF0) >> 4));
            Nibble rightNibble = Nibble.fromByte((byte) (nextByte & 0x0F));

            this.remainingNibble = rightNibble;

            return leftNibble;
        }
    }
}

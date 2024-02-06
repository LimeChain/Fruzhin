package com.limechain.trie.structure.nibble;

import org.apache.commons.collections4.IteratorUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.stream.Stream;

/**
 * Convenience wrapper for any 'sequence of Nibble'-like structure.
 * It's immutable and its public static factory methods eagerly copy passed data (nibbles) to obtain ownership.
 */
public class Nibbles implements Iterable<Nibble>, RandomAccess, Comparable<Iterable<Nibble>> {
    /**
     * A sequence of zero nibbles, i.e. empty
     */
    public static final Nibbles EMPTY = new Nibbles(List.of());

    /**
     * A sequence of all nibbles in order, from 0 to F
     */
    public static final Nibbles ALL = Nibble.all().collect(NibblesCollector.toNibbles());

    private final List<Nibble> entries;

    public static Nibbles of(Nibble nibble) {
        return Nibbles.of(List.of(nibble));
    }

    public static Nibbles of(Nibble[] nibbles) {
        return Nibbles.of(Arrays.asList(nibbles));
    }

    public static Nibbles of(Collection<Nibble> nibbles) {
        return new Nibbles(new ArrayList<>(nibbles));
    }

    public static Nibbles of(Stream<Nibble> nibbles) {
        return Nibbles.of(nibbles.iterator());
    }

    public static Nibbles of(Iterable<Nibble> nibbles) {
        return Nibbles.of(nibbles.iterator());
    }

    public static Nibbles of(Iterator<Nibble> nibbles) {
        return new Nibbles(IteratorUtils.toList(nibbles));
    }

    /**
     * Creates Nibbles from a given byte array by using the {@link BytesToNibbles} iterator.
     *
     * @param bytes arrays of bytes to convert
     * @return Nibbles representation
     */
    public static Nibbles fromBytes(byte[] bytes) {
        return Nibbles.of(new BytesToNibbles(bytes));
    }

    Nibbles(List<Nibble> entries) {
        this.entries = entries;
    }

    /**
     * @return Nibbles constructed from a string of hexadecimal characters (nibbles).
     * The capitalization of the characters doesn't matter.
     */
    public static Nibbles fromHexString(String hex) {
        return hex.chars()
            .mapToObj(c -> Nibble.fromAsciiHexDigit((char) c))
            .collect(NibblesCollector.toNibbles());
    }

    /**
     * Whether this nibbles starts with the given prefix.
     */
    public boolean startsWith(Nibbles prefix) {
        final int thisSize = this.size();
        final int prefixSize = prefix.size();

        if (prefixSize > thisSize) {
            return false;
        }

        return prefix.entries.equals(this.entries.subList(0, prefixSize));
    }

    /**
     * @return the lower hexadecimal string representation of this Nibbles
     */
    public String toLowerHexString() {
        return NibblesUtils.toLowerHexString(this.entries);
    }

    /**
     * Adds a new nibble to the nibbles.
     * @param nibble the new nibble to add
     * @return the new Nibbles after insertion
     * @implNote will create a new modified copy of the nibbles, does not mutate the instance invoked on
     */
    public Nibbles add(Nibble nibble) {
        List<Nibble> newNibbles = new ArrayList<>(this.entries);
        newNibbles.add(nibble);
        return new Nibbles(newNibbles);
    }

    /**
     * Adds a new nibble to the nibbles at the given position.
     * @param nibble the new nibble to add
     * @return the new Nibbles after insertion
     * @implNote will create a new modified copy of the nibbles, does not mutate the instance invoked on
     */
    public Nibbles add(int index, Nibble nibble) {
        List<Nibble> newNibbles = new ArrayList<>(this.entries);
        newNibbles.add(index, nibble);
        return new Nibbles(newNibbles);
    }

    /**
     * Returns the element at the specified position in this list.
     *
     * @param index index of the element to return
     * @return the element at the specified position in this list
     * @throws IndexOutOfBoundsException if the index is out of range
     *         ({@code index < 0 || index >= size()})
     */
    public Nibble get(int index) {
        return entries.get(index);
    }

    // NOTE:
    //  Since Nibbles is immutable, we don't actually need to explicitly copy. Passing by reference suffices.
    //  This method would've been removed during refactoring to immutability, but we've left it out as a clear marker
    //  of where ownership changes occur, so that if one day efficiency becomes a problem and we want to refactor again
    //  in favor of mutability, we won't have to deduce our way back the hard way.
    /**
     * Since {@link Nibbles} is immutable, returns {@code this}.
     * Exists only as a marker for copying in order to trace ownership more explicitly.
     * @return this
     */
    public Nibbles copy() {
        return this;
    }

    /**
     * Drops a number of nibbles from the beginning
     * @param n the number of nibbles to skip
     * @return a new Nibbles without the first n leading nibbles
     * @throws IndexOutOfBoundsException if
     *         ({@code n < 0 || n > size})
     */
    public Nibbles drop(int n) {
        return new Nibbles(this.entries.subList(n, this.entries.size()));
    }

    /**
     * Takes only the first n nibbles from the beginning.
     * @param n the number of nibbles to take
     * @return a new Nibbles limited to only the first n leading nibbles
     * @throws IndexOutOfBoundsException if
     *         ({@code n < 0 || n > size})
     */
    public Nibbles take(int n) {
        return new Nibbles(this.entries.subList(0, n));
    }

    /**
     * Returns the number of elements in this Nibbles. If it contains
     * more than {@code Integer.MAX_VALUE} elements, returns
     * {@code Integer.MAX_VALUE}.
     *
     * @return the number of elements in this Nibbles
     */
    public int size() {
        return entries.size();
    }

    /**
     * @return true if this Nibbles contains no elements
     */
    public boolean isEmpty() {
        return entries.isEmpty();
    }

    /**
     * @return an unmodifiable view of the underlying list of nibbles
     */
    public List<Nibble> asUnmodifiableList() {
        return Collections.unmodifiableList(this.entries);
    }

    /**
     * @return a stream of the contained nibbles
     */
    public Stream<Nibble> stream() {
        return entries.stream();
    }

    @NotNull
    public Iterator<Nibble> iterator() {
        return entries.iterator();
    }

    @Override
    public int compareTo(@NotNull Iterable<Nibble> o) {
        // NOTE: Could be made more efficient by avoiding the string serialization, if it ever becomes an issue.
        return this.toLowerHexString().compareTo(NibblesUtils.toLowerHexString(o));
    }

    @Override
    public String toString() {
        return this.toLowerHexString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Nibbles nibbles1 = (Nibbles) o;
        return this.entries.equals(nibbles1.entries);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.entries);
    }
}

package com.limechain.trie.structure.nibble;

import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.stream.Collector;
import java.util.stream.Stream;

// TODO:
//  Should this implement List or just Iterable... or maybe extend List<Nibble>?
//  This current version contains a huge amount of boilerplate,
//  but it's what I've reached as a most convenient option to use outside.
//  Basically a List<Nibble> with additional methods.

// TODO:
//  As a List<Nibble> implementor, Nibbles is currently mutable, which I severely dislike,
//  since this mandates the developer to think about where to copy and where passing by reference is sufficient.

/**
 * Convenience wrapper for any 'sequence of Nibble'-like structure
 */
public class Nibbles implements List<Nibble>, RandomAccess, Comparable<List<Nibble>> {
    public static final Nibbles EMPTY = new Nibbles(new byte[]{});

    /**
     * Don't mutate! :D
     */
    public static final Nibbles ALL = new Nibbles(Nibble.all());

    public static Stream<Nibble> all() {
        return ALL.stream();
    }

    private final List<Nibble> nibbles;

    // TODO: Maybe convert all those overloaded constructors to static generational methods
    public Nibbles(Nibble nibble) {
        this(List.of(nibble));
    }

    public Nibbles(Nibble[] nibbles) {
        this(Arrays.asList(nibbles));
    }

    public Nibbles(byte[] nibbles) {
        this(Arrays.stream(ArrayUtils.toObject(nibbles)).map(Nibble::fromByte).toList());
    }

    public Nibbles(Iterable<Nibble> nibbles) {
        this(nibbles.iterator());
    }

    public Nibbles(Stream<Nibble> nibbles) {
        this(nibbles.iterator());
    }

    public static Nibbles of(int... nibbles) {
        return new Nibbles(Arrays.stream(nibbles).mapToObj(Nibble::fromInt));
    }

    public Nibbles(Iterator<Nibble> nibbles) {
        // TODO:
        //  Reconsider the internal representation
        //  LinkedList seems appropriate since we'll mostly iterate from left to right...
        //  but for now we're sticking with ArrayList (returned from IteratorUtils.toList)

        // TODO:
        //  Reconsider whether this class should eagerly copy outer data to gain ownership
        //  or simply trust the caller for `read-only` invariance of the given references...
        //  For now, we simply copy references. Could lead to problems if a Nibble is mutated outside...
        this.nibbles = IteratorUtils.toList(nibbles);
    }

    public boolean startsWith(Nibbles prefix) {
        final int thisSize = this.size();
        final int prefixSize = prefix.size();

        if (prefixSize > thisSize) {
            return false;
        }

        return prefix.nibbles.equals(this.nibbles.subList(0, prefixSize));
    }

    @NotNull
    @Override
    public Iterator<Nibble> iterator() {
        return nibbles.iterator();
    }

    /**
     * @return the lower hexadecimal string representation of this Nibbles
     */
    public String toLowerHexString() {
        return toLowerHexString(this.nibbles);
    }

    // TODO:
    //  This methods exists simply because to Java, not any `List<Nibble>` is the same as `Nibbles`
    //  Although that's exactly what I want :D
    private static String toLowerHexString(List<Nibble> nibbles) {
        return nibbles.stream()
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

    @Override
    public String toString() {
        return this.toLowerHexString();
    }

    @Override
    public Stream<Nibble> stream() {
        return nibbles.stream();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Nibbles nibbles1 = (Nibbles) o;
        return this.nibbles.equals(nibbles1.nibbles);
    }


    @Override
    public int hashCode() {
        return Objects.hash(this.nibbles);
    }

    @NotNull
    @Override
    public Object[] toArray() {
        return nibbles.toArray();
    }

    @NotNull
    @Override
    public <T> T[] toArray(@NotNull T[] a) {
        return nibbles.toArray(a);
    }

    @Override
    public boolean add(Nibble nibble) {
        return nibbles.add(nibble);
    }

    @Override
    public boolean remove(Object o) {
        return nibbles.remove(o);
    }

    @Override
    public boolean containsAll(@NotNull Collection<?> c) {
        return new HashSet<>(nibbles).containsAll(c);
    }

    @Override
    public boolean addAll(@NotNull Collection<? extends Nibble> c) {
        return nibbles.addAll(c);
    }

    @Override
    public boolean addAll(int index, @NotNull Collection<? extends Nibble> c) {
        return nibbles.addAll(c);
    }

    @Override
    public boolean removeAll(@NotNull Collection<?> c) {
        return nibbles.removeAll(c);
    }

    @Override
    public boolean retainAll(@NotNull Collection<?> c) {
        return nibbles.retainAll(c);
    }

    @Override
    public void clear() {
        nibbles.clear();
    }

    @Override
    public Nibble get(int index) {
        return nibbles.get(index);
    }

    @Override
    public Nibble set(int index, Nibble element) {
        return nibbles.get(index);
    }

    @Override
    public void add(int index, Nibble element) {
        nibbles.add(index, element);
    }

    @Override
    public Nibble remove(int index) {
        return nibbles.remove(index);
    }

    @Override
    public int indexOf(Object o) {
        return nibbles.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return nibbles.lastIndexOf(o);
    }

    @NotNull
    @Override
    public ListIterator<Nibble> listIterator() {
        return nibbles.listIterator();
    }

    @NotNull
    @Override
    public ListIterator<Nibble> listIterator(int index) {
        return nibbles.listIterator();
    }

    @NotNull
    @Override
    public List<Nibble> subList(int fromIndex, int toIndex) {
        return nibbles.subList(fromIndex, toIndex);
    }

    public Nibbles copy() {
        return new Nibbles(this.nibbles);
    }

    @Override
    public int size() {
        return nibbles.size();
    }

    public boolean isEmpty() {
        return nibbles.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return nibbles.contains(o);
    }

    public List<Nibble> asUnmodifiableList() {
        return Collections.unmodifiableList(this.nibbles);
    }

    @Override
    public int compareTo(@NotNull List<Nibble> o) {
        return this.toLowerHexString().compareTo(toLowerHexString(o));
    }

    /**
     * @return Nibbles constructed from a string of hexadecimal characters (nibbles).
     * The capitalization of the characters doesn't matter.
     */
    public static Nibbles fromHexString(String hex) {
        return new Nibbles(hex.chars().mapToObj(c -> (char) c).map(Nibble::fromAsciiHexDigit));
    }
}

package com.limechain.trie.structure.nibble;

import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

/**
 * Convenience wrapper for any 'sequence of Nibble'-like structure
 */
//TODO:
// Should this implement List or just Iterable... or maybe extend List<Nibble>?
// This current version contains a huge amount of boilerplate,
// but it's what I've reached as a most convenient option to use outside.
// Basically a List<Nibble> with additional methods.
public class Nibbles implements List<Nibble> {
    public static final Nibbles EMPTY = new Nibbles(new byte[]{});

    private final List<Nibble> nibbles;

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

    public Nibbles(Iterator<Nibble> nibbles) {
        // TODO:
        //  Reconsider the internal representation
        //  LinkedList seems appropriate since we'll mostly iterate from left to right...
        //  but for now we're sticking with ArrayList (returned from IteratorUtils.toList)

        // TODO:
        //  Reconsider whether this class should eagerly copy outer data to gain ownership
        //  or simply trust the caller for `read-only` invariance of the given references...
        //  For now, we copy everything.
        this.nibbles = IteratorUtils.toList(nibbles);
    }

    public boolean startsWith(Nibbles prefix) {
        final int thisSize = this.size();
        final int prefixSize = prefix.size();

        if (prefixSize > thisSize) {
            return false;
        }

        return prefix.equals(this.subList(0, prefixSize));
    }

    @NotNull
    @Override
    public Iterator<Nibble> iterator() {
        return nibbles.iterator();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Nibbles nibbles1 = (Nibbles) o;
        return Objects.equals(nibbles, nibbles1.nibbles);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nibbles);
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

    //TODO: Maybe implement Cloneable?
    public Nibbles clone() {
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
}

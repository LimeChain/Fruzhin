package com.limechain.trie.structure.slab;

import com.limechain.trie.structure.slab.exceptions.InvalidSlabIndexException;
import org.javatuples.Pair;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Represents a slab data structure for efficient management of a fixed type of objects.
 * It provides methods for adding, removing, and accessing elements, along with utilities
 * for managing the slab's capacity and contents.
 *
 * @param <T> the type of elements held in this slab
 */
public class Slab<T> implements Iterable<Pair<Integer, T>> {
    private ArrayList<T> storage;
    private int newIndex;
    private Queue<Integer> freeIndices;
    private int size;

    /**
     * Constructs an empty Slab with default initial capacity of 10.
     */
    public Slab() {
        initialize(10);
    }

    /**
     * Constructs a Slab with the specified initial capacity.
     *
     * @param initialCapacity the initial capacity of the slab
     */
    public Slab(int initialCapacity) {
        initialize(initialCapacity);
    }

    /**
     * Initializes the internal storage structures of the slab.
     *
     * @param initialCapacity the initial capacity of the slab
     */
    private void initialize(int initialCapacity) {
        this.storage = new ArrayList<>(initialCapacity);
        this.newIndex = 0;
        this.size = 0;
        this.freeIndices = new LinkedList<>();
    }

    /**
     * Adds an element to the slab and returns its index.
     *
     * @param element the element to be added, must not be null
     * @return the index at which the element is stored
     * @throws NullPointerException if the element is null
     */
    public int add(@NotNull T element) {
        int index;
        if (freeIndices.isEmpty()) {
            index = newIndex;
            storage.add(element);
            newIndex++;
        } else {
            index = freeIndices.poll();
            storage.set(index, element);
        }
        size++;
        return index;
    }

    /**
     * Removes the element at the specified index from the slab.
     *
     * @param index the index of the element to be removed
     * @return the element previously at the specified index
     */
    public @NotNull T remove(int index) {
        T value = this.get(index);
        storage.set(index, null);
        freeIndices.add(index);
        size--;
        return value;
    }

    /**
     * Retrieves an element from the storage at the specified index.
     *
     * @param index The index of the element to be retrieved.
     * @return The element at the specified index.
     * @throws InvalidSlabIndexException If the index is out of bounds or if the value at the index is null.
     *                                   This exception wraps the original {@code IndexOutOfBoundsException}
     *                                   when the index is out of bounds.
     */
    public @NotNull T get(int index) {
        if (index < 0 || index >= storage.size()) {
            throw new InvalidSlabIndexException("Index " + index + " out of bounds for underlying storage.");
        }
        T value = storage.get(index);
        if (value == null) {
            throw new InvalidSlabIndexException("Index " + index + " does not return any value.");
        }
        return value;
    }

    /**
     * Removes all elements from the slab.
     */
    public void clear() {
        initialize(0);
    }

    /**
     * Returns a list of all non-null elements in the slab along with their indices.
     *
     * @return a list of pairs, where each pair contains an index and a non-null element
     */
    public List<Pair<Integer, T>> getAllEntries() {
        return streamAllEntries().toList();
    }

    /**
     * Removes all elements from the slab and returns them as a list.
     *
     * @return a list of pairs, where each pair contains an index and an element,
     * representing all elements removed from the slab
     */
    public List<Pair<Integer, T>> drain() {
        var result = getAllEntries();
        clear();
        return result;
    }

    /**
     * Checks if the slab is empty.
     *
     * @return true if the slab contains no elements
     */
    public boolean isEmpty() {
        return storage.isEmpty();
    }

    /**
     * Returns the number of elements in the slab.
     *
     * @return the number of elements in the slab
     */
    public int size() {
        return size;
    }

    /**
     * Provides a stream of all non-null elements in the slab along with their indices.
     *
     * @return a stream of pairs, where each pair contains an index and a non-null element
     */
    private Stream<Pair<Integer, T>> streamAllEntries() {
        return IntStream.range(0, storage.size())
                .mapToObj(i -> new Pair<>(i, storage.get(i)))
                .filter(p -> Objects.nonNull(p.getValue1()));
    }

    /**
     * Returns an iterator over elements of type {@code Pair<Integer, T>}.
     * Each pair contains an index and a non-null element from the slab.
     *
     * @return an Iterator.
     */
    @NotNull
    @Override
    public Iterator<Pair<Integer, T>> iterator() {
        return streamAllEntries().iterator();
    }
}

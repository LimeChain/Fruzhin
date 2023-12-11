package com.limechain.trie.structure.slab;

import lombok.NonNull;
import org.javatuples.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Slab<T> implements Iterable<Pair<Integer, T>> {
    private ArrayList<T> storage;
    private int newIndex;
    private Queue<Integer> freeIndices;

    public Slab() {
        initialize(0);
    }

    public Slab(int initialCapacity) {
        initialize(initialCapacity);
    }

    private void initialize(int initialCapacity) {
        this.storage = new ArrayList<>(initialCapacity);
        this.newIndex = 0;
        this.freeIndices = new LinkedList<>();
    }

    public int add(@NonNull T element) {
        int index = freeIndices.isEmpty() ? newIndex++ : freeIndices.poll();
        storage.add(index, element);
        return index;
    }

    public T remove(int index) {
        T value = storage.get(index);
        storage.set(index, null);
        freeIndices.add(index);
        return value;
    }

    public @Nullable T get(int index) {
        return storage.get(index);
    }

    public void clear() {
        initialize(0);
    }

    public List<Pair<Integer, T>> getAllEntries() {
        return streamAllEntries().collect(Collectors.toList());
    }

    public List<Pair<Integer, T>> drain() {
        var result = getAllEntries();
        clear();
        return result;
    }

    public boolean isEmpty() {
        return storage.isEmpty();
    }

    public int size() {
        return storage.size();
    }

    private Stream<Pair<Integer, T>> streamAllEntries() {
        return IntStream.range(0, storage.size())
            .mapToObj(i -> new Pair<>(i, storage.get(i)))
            .filter(p -> Objects.nonNull(p.getValue1()));
    }

    @NotNull
    @Override
    public Iterator<Pair<Integer, T>> iterator() {
        return streamAllEntries().iterator();
    }
}

package com.limechain.trie.structure;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

public class TrieStructure<T> implements Iterable<Integer> {
    @NotNull
    @Override
    public Iterator<Integer> iterator() {
        return null;
    }

    public T getUserDataAtIndex(int i) {
        return (T) new Object();
    }
}

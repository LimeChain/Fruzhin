package com.limechain.trie.cache;

import com.limechain.trie.structure.nibble.Nibbles;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Stream;

/**
 * Container used to cache the difference between two versions of a trie. For example this can be used when the runtime
 * edits the storage via host api calls, block execution, etc. The aim of this is to lower the number of expensive
 * operations towards an on disk merkle trie.
 * @param <T> class type of additionally stored user data.
 */
@RequiredArgsConstructor
public class TrieDiff<T> {

    /**
     * Same data as in 'hashmap' field, but using booleans to denote. A false value for deletion, true for update.
     */
    private final TreeMap<Nibbles, Boolean> btree;
    /**
     * Stores the difference between the trie and its previous version. The {@link TrieDiffEntry}'s value field shows
     * if the operation is an update or a deletion. A 'null' value denotes deletion and a 'non-null' is an update.
     */
    private final HashMap<Nibbles, TrieDiffEntry<T>> hashmap;

    // Clears all entries in the TrieDiff
    public void clear() {
        hashmap.clear();
        btree.clear();
    }

    public Optional<TrieDiffEntry<T>> diffInsert(Nibbles key, byte[] value, T userData) {
        TrieDiffEntry<T> previous = hashmap.put(key, new TrieDiffEntry<>(value, userData));
        if (previous == null || previous.value == null) {
            btree.put(key, true);
        }
        return Optional.ofNullable(previous);
    }

    public Optional<TrieDiffEntry<T>> diffInsertErase(Nibbles key, T userData) {
        TrieDiffEntry<T> previous = hashmap.put(key, new TrieDiffEntry<>(null, userData));
        if (previous == null || previous.value != null) {
            btree.put(key, false);
        }
        return Optional.ofNullable(previous);
    }

    public Optional<TrieDiffEntry<T>> diffRemove(Nibbles key) {
        TrieDiffEntry<T> previous = hashmap.remove(key);
        if (previous != null) {
            btree.remove(key);
        }
        return Optional.ofNullable(previous);
    }

    public Optional<TrieDiffEntry<T>> diffGet(Nibbles key) {
        return Optional.ofNullable(hashmap.get(key));
    }

    public Stream<Map.Entry<Nibbles, TrieDiffEntry<T>>> diffIterUnordered() {
        return hashmap.entrySet().stream();
    }

    public Stream<Map.Entry<Nibbles, Boolean>> diffRangeOrdered(Nibbles fromKey, Nibbles toKey) {
        return btree.subMap(fromKey, true, toKey, true).entrySet().stream();
    }

    public record TrieDiffEntry<T>(@Nullable byte[] value, T userData) {
    }
}
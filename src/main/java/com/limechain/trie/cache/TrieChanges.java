package com.limechain.trie.cache;

import com.limechain.trie.cache.node.PendingInsertUpdate;
import com.limechain.trie.cache.node.PendingRemove;
import com.limechain.trie.cache.node.PendingTrieNodeChange;
import com.limechain.trie.structure.nibble.Nibbles;
import com.limechain.trie.structure.node.InsertUpdate;
import com.limechain.trie.structure.node.Remove;
import com.limechain.trie.structure.node.TrieNodeChange;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;

/**
 * Container used to cache the difference between two versions of a trie. For example this can be used when the runtime
 * edits the storage via host api calls, block execution, etc. The aim of this is to lower the number of expensive
 * operations towards an on disk merkle trie.
 *
 * @param <T> type of object used as additional user data in {@link TrieDiffEntry}.
 */
@RequiredArgsConstructor
public class TrieChanges<T> {

    /**
     * Same data as in 'hashmap' field, but using booleans to denote. A false value for deletion, true for update.
     */
    private final TreeMap<Nibbles, Boolean> btree;
    /**
     * Stores the difference between the trie and its previous version. The {@link TrieDiffEntry}'s value field shows
     * if the operation is an update or a deletion.
     */
    private final HashMap<Nibbles, TrieDiffEntry<T>> hashmap;

    /**
     * Holds data for node changes identified by their path.
     */
    private final TreeMap<Nibbles, PendingTrieNodeChange> triesChanges;

    public static <T> TrieChanges<T> empty() {
        return new TrieChanges<>(new TreeMap<>(), new HashMap<>(), new TreeMap<>());
    }

    /**
     * Returns the newly stored value if any for a key in the trie.
     *
     * @param key {@link Nibbles} used to identify a node inside the diff.
     * @return An Optional with the newly stored value or an empty optional if none is found.
     */
    public Optional<byte[]> trieDiffGet(Nibbles key) {
        return Optional.ofNullable(hashmap.get(key))
            .map(TrieDiffEntry::value);
    }

    public void diffInsert(Nibbles key, byte[] value, @Nullable T userData) {
        TrieDiffEntry<T> previous = hashmap.put(key, new TrieDiffEntry<>(value, userData));
        if (previous == null || previous.value == null) {
            btree.put(key, true);
        }
    }

    public void diffInsertErase(Nibbles key, @Nullable T userData) {
        TrieDiffEntry<T> previous = hashmap.put(key, new TrieDiffEntry<>(null, userData));
        if (previous == null || previous.value != null) {
            btree.put(key, false);
        }
    }

    /**
     * @return An iterator over the changes performed on the trie in an ordered fashion.
     */
    public Iterator<Map.Entry<Nibbles, TrieNodeChange>> trieChangesIterOrdered() {
        List<Map.Entry<Nibbles, TrieNodeChange>> changes = new ArrayList<>();

        for (Map.Entry<Nibbles, PendingTrieNodeChange> entry : triesChanges.entrySet()) {
            Nibbles key = entry.getKey();
            PendingTrieNodeChange pendingChange = entry.getValue();
            TrieNodeChange change;

            if (pendingChange instanceof PendingRemove) {
                change = new Remove();
            } else if (pendingChange instanceof PendingInsertUpdate(
                byte[] merkle,
                List<byte[]> childMerkles,
                Nibbles pk
            )) {
                Optional<TrieDiffEntry<T>> diffEntry = Optional.ofNullable(hashmap.get(key));

                byte[] newStorageValue = null;
                if (diffEntry.isPresent()) {
                    newStorageValue = diffEntry.get().value();
                }

                change = new InsertUpdate(merkle, childMerkles, pk, newStorageValue);
            } else {
                continue;
            }
            changes.add(Map.entry(key, change));
        }

        return changes.iterator();
    }

    private record TrieDiffEntry<T>(@Nullable byte[] value, @Nullable T userData) {

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TrieDiffEntry<?> that = (TrieDiffEntry<?>) o;
            return Objects.equals(userData, that.userData) && Objects.deepEquals(value, that.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(Arrays.hashCode(value), userData);
        }

        @Override
        public String toString() {
            return "TrieDiffEntry{" +
                "value=" + Arrays.toString(value) +
                ", userData=" + userData +
                '}';
        }
    }
}

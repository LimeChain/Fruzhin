package com.limechain.trie.cache;

import com.limechain.trie.cache.node.PendingInsertUpdate;
import com.limechain.trie.cache.node.PendingRemove;
import com.limechain.trie.cache.node.PendingTrieNodeChange;
import com.limechain.trie.structure.nibble.Nibbles;
import com.limechain.trie.structure.node.InsertUpdate;
import com.limechain.trie.structure.node.Remove;
import com.limechain.trie.structure.node.TrieNodeChange;
import lombok.RequiredArgsConstructor;

import java.util.*;

/**
 * This class serves the purpose of a container for the changes that a block performs on the storage during execution.
 *
 * @param <T> type of object used as additional user data in {@link TrieDiff}.
 */
@RequiredArgsConstructor
public class TrieChanges<T> {

    /**
     * Holds data values written to/deleted from each trie. A 'null' key denotes the main trie. Children tries are
     * identified by their corresponding merkle roots.
     */
    private final Map<byte[], TrieDiff<T>> trieDiffs;
    /**
     * Set of trie merkle values that need to be recalculated. A 'null' value denotes the main trie.
     */
    private final Set<byte[]> staleTriesRoots;
    /**
     * Holds data for node changes identified by their trie merkle value ('null' for main trie) and path.
     */
    private final TreeMap<TrieKey, PendingTrieNodeChange> triesChanges;

    public static <T> TrieChanges<T> empty() {
        return new TrieChanges<>(new HashMap<>(), new HashSet<>(), new TreeMap<>());
    }

    /**
     * Returns the newly stored value if any for a key in the main trie.
     *
     * @param key {@link Nibbles} used to identify a node inside the diff.
     * @return An Optional with the newly stored value or an empty optional if none is found.
     */
    public Optional<byte[]> mainTrieDiffGet(Nibbles key) {
        return Optional.ofNullable(trieDiffs.get(null))
            .flatMap(diff -> diff.diffGet(key)
                .map(TrieDiff.TrieDiffEntry::value));
    }

    /**
     * @return an iterator over the changes performed on all the tries ('null' for main) in an ordered fashion.
     * See {@link TrieKey} for reference on ordering.
     */
    public Iterator<Map.Entry<TrieKey, TrieNodeChange>> trieChangesIterOrdered() {
        List<Map.Entry<TrieKey, TrieNodeChange>> changes = new ArrayList<>();

        for (Map.Entry<TrieKey, PendingTrieNodeChange> entry : triesChanges.entrySet()) {
            TrieKey key = entry.getKey();
            PendingTrieNodeChange pendingChange = entry.getValue();
            TrieNodeChange change;

            if (pendingChange instanceof PendingRemove) {
                change = new Remove();
            } else if (pendingChange instanceof PendingInsertUpdate(byte[] merkle,
                                                                    Nibbles pk,
                                                                    List<byte[]> childMerkles)) {
                Optional<TrieDiff.TrieDiffEntry<T>> diffEntry = trieDiffs.get(key.trieIdentifier)
                    .diffGet(key.keyPath);

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

    /**
     * @param trieIdentifier Could be the root hash of the child trie
     * @param keyPath        Path to the node within the trie
     */
    public record TrieKey(byte[] trieIdentifier, Nibbles keyPath) implements Comparable<TrieKey> {
        // Implement comparison for ordering
        @Override
        public int compareTo(TrieKey other) {
            int cmp = compareByteArrays(this.trieIdentifier, other.trieIdentifier);
            if (cmp != 0) {
                return cmp;
            }

            return keyPath.compareTo(other.keyPath);
        }

        private int compareByteArrays(byte[] a, byte[] b) {
            if (a == b) return 0;
            if (a == null) return -1;
            if (b == null) return 1;
            int minLength = Math.min(a.length, b.length);
            for (int i = 0; i < minLength; i++) {
                int cmp = Byte.compare(a[i], b[i]);
                if (cmp != 0) return cmp;
            }
            return Integer.compare(a.length, b.length);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TrieKey trieKey = (TrieKey) o;
            return Objects.equals(keyPath, trieKey.keyPath)
                && Objects.deepEquals(trieIdentifier, trieKey.trieIdentifier);
        }

        @Override
        public int hashCode() {
            return Objects.hash(Arrays.hashCode(trieIdentifier), keyPath);
        }

        @Override
        public String toString() {
            return "TrieKey{" +
                "trieIdentifier=" + Arrays.toString(trieIdentifier) +
                ", keyPath=" + keyPath +
                '}';
        }
    }
}

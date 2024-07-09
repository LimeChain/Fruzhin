package com.limechain.trie;

import com.limechain.runtime.version.StateVersion;
import com.limechain.storage.DeleteByPrefixResult;
import com.limechain.storage.trie.TrieStorage;
import com.limechain.trie.structure.nibble.Nibbles;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * The interface used for various trie implementations. Currently, 2 exist:<br>
 * {@link MemoryTrieAccessor} - an in-memory trie implementation.<br>
 * {@link DiskTrieAccessor} - an on-disk trie implementation.
 */
public abstract sealed class TrieAccessor permits MemoryTrieAccessor, DiskTrieAccessor {

    private static final String TRANSACTIONS_NOT_SUPPORTED = "Block Trie Accessor does not support transactions.";

    protected final TrieStorage trieStorage;
    protected final Map<Nibbles, TrieAccessor> loadedChildTries;
    protected byte[] mainTrieRoot;
    @Setter
    protected StateVersion currentStateVersion;

    protected TrieAccessor(TrieStorage trieStorage, byte[] mainTrieRoot) {
        this.trieStorage = trieStorage;
        this.mainTrieRoot = mainTrieRoot;
        this.loadedChildTries = new HashMap<>();
    }

    /**
     * Updates/Inserts a node in the trie implementation.
     *
     * @param key   The key to save.
     * @param value The value to save.
     */
    abstract void upsertNode(Nibbles key, byte[] value);

    /**
     * Deletes the value associated with the given key from the trie implementation.
     *
     * @param key The key to delete.
     */
    abstract void deleteNode(Nibbles key);

    /**
     * Finds the value associated with the given key in the trie implementation.
     *
     * @param key The key to search for.
     * @return An Optional containing the value if found, or empty otherwise.
     */
    abstract Optional<byte[]> findStorageValue(Nibbles key);

    /**
     * Deletes nodes in the trie implementation that match the given prefix.
     *
     * @param prefix The prefix to match for deletion.
     * @param limit  The maximum number of keys to delete.
     * @return A DeleteByPrefixResult indicating the number of keys deleted and whether all keys were deleted.
     */
    abstract DeleteByPrefixResult deleteMultipleNodesByPrefix(Nibbles prefix, Long limit);

    /**
     * Finds the smallest key in the Trie that is lexicographically greater than the given one.
     *
     * @param key the key to compare against for finding the next greater key.
     * @return an {@code Optional<Nibbles>} containing the next greater key if found,
     * otherwise an empty {@code Optional}.
     */
    abstract Optional<Nibbles> getNextKey(Nibbles key);

    /**
     * Persists the accumulated changes to the underlying database storage.
     */
    void persistChanges() {
        for (TrieAccessor value : loadedChildTries.values()) value.persistChanges();
        loadedChildTries.clear();
    }

    /**
     * Retrieves the child trie accessor for the given key.
     *
     * @param key The key corresponding to the child trie accessor.
     * @return The TrieAccessor of a child trie for the specified key.
     */
    abstract TrieAccessor getChildTrie(Nibbles key);

    /**
     * Starts a transaction, that can later be committed or rolled back
     */
    public void startTransaction() {
        throw new UnsupportedOperationException(TRANSACTIONS_NOT_SUPPORTED);
    }

    /**
     * Rollbacks an active transaction, discarding changes.
     */
    public void rollbackTransaction() {
        throw new UnsupportedOperationException(TRANSACTIONS_NOT_SUPPORTED);
    }

    /**
     * Commits an active transaction, persisting changes.
     */
    public void commitTransaction() {
        throw new UnsupportedOperationException(TRANSACTIONS_NOT_SUPPORTED);
    }
}

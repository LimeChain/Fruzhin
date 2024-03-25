package com.limechain.storage;

import org.springframework.lang.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Key-Value DB Interface
 *
 * @param <K> type of the key
 * @param <V> type of the value
 */
public interface KVRepository<K, V> {
    /**
     * Persists a key-value pair to the DB
     *
     * @param key   key of the pair
     * @param value value of the pair
     * @return whether the save operation was successful
     */
    boolean save(K key, V value);

    /**
     * Tries to find a value for a given key in the DB
     *
     * @param key the key to search for
     * @return Optional result that could contain the value
     */
    Optional<V> find(K key);

    /**
     * Deletes a key-value pair from the DB
     *
     * @param key the key of the pair
     * @return whether the delete operation was successful
     */
    boolean delete(K key);

    /**
     * Finds all keys sharing a common prefix up to a given limit.
     *
     * @param prefixSeek prefix of the key to look for
     * @param limit maximum keys to return
     * @return whether the delete operation was successful
     */
    List<byte[]> findKeysByPrefix(K prefixSeek, int limit);

    /**
     * Deletes key-value pairs from the DB where key starts with prefix, up to a given limit.
     *
     * @param prefix prefix for the key of the pair
     * @param limit maximum entries to delete
     * @return how many entries were deleted and if all were deleted
     */
    DeleteByPrefixResult deleteByPrefix(K prefix, @Nullable Long limit);

    /**
     * Tries to find the next key after a given key in the DB
     *
     * @param key the key to search for
     * @return Optional result that could contain the value
     */
    Optional<K> getNextKey(K key);

    /**
     * Starts a DB transaction, that can later be committed or rolled back
     */
    void startTransaction();

    /**
     * Rollbacks an active DB transaction, discarding changes.
     */
    void rollbackTransaction();

    /**
     * Commits an active DB transaction, persisting changes.
     */
    void commitTransaction();

    /**
     * Closes the connection to the DB
     */
    void closeConnection();
}


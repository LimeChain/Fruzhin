package com.limechain.storage;

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
     * Deletes a key-value pair from the DB for the n(limit) key starting with prefix
     *
     * @param prefixSeek prefix for the key of the pair
     * @param limit maximum entries to delete
     * @return whether the delete operation was successful
     */
    int deletePrefix(String prefixSeek, int limit);

    /**
     * Closes the connection to the DB
     */
    void closeConnection();
}


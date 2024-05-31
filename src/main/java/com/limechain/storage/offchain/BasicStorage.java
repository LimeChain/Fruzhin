package com.limechain.storage.offchain;

import java.util.Arrays;

/**
 * A minimal interface for a basic key-value storage.
 */
public interface BasicStorage {
    // TODO: Shouldn't these `key`s be byte[] instead of String? `String` is not a good container for raw `byte[]`s.
    void set(String key, byte[] value);
    byte[] get(String key);
    void remove(String key);

    /**
     * A default, thread-safe implementation of 'compare and set'.
     * Checks the current value for a `key` and if it matches `oldValue`, replaces the value with `newValue`.
     * Otherwise, the current value remains, i.e. does nothing.
     * @param key the key whose value to check
     * @param oldValue the expected old value to check against
     * @param newValue the new value to replace if old value matches
     * @return true if the old value was replaced with the new one; false otherwise
     */
    default boolean compareAndSet(String key, byte[] oldValue, byte[] newValue) {
        synchronized (this) {
            byte[] currentValue = this.get(key);

            if (!Arrays.equals(oldValue, currentValue)) {
                return false;
            }

            this.set(key, newValue);
            return true;
        }
    }
}

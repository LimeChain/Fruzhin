package com.limechain.trie.dto.node;

import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Objects;

public record StorageValue(@NotNull byte[] value, boolean isHashed) {
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StorageValue that = (StorageValue) o;
        return isHashed == that.isHashed && Arrays.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(isHashed);
        result = 31 * result + Arrays.hashCode(value);
        return result;
    }

    @Override
    public String toString() {
        return "StorageValue{" +
               "value=" + Arrays.toString(value) +
               ", isHashed=" + isHashed +
               '}';
    }
}

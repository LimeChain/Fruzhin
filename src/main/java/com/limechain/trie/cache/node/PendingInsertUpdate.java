package com.limechain.trie.cache.node;

import com.limechain.trie.structure.nibble.Nibbles;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public record PendingInsertUpdate(byte[] newMerkleValue, List<byte[]> childrenMerkleValues,
                                  Nibbles partialKey) implements PendingTrieNodeChange {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PendingInsertUpdate that = (PendingInsertUpdate) o;
        return Objects.equals(partialKey, that.partialKey)
            && Objects.deepEquals(newMerkleValue, that.newMerkleValue)
            && Objects.equals(childrenMerkleValues, that.childrenMerkleValues);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(newMerkleValue), partialKey, childrenMerkleValues);
    }

    @Override
    public String toString() {
        return "PendingInsertUpdate{" +
            "newMerkleValue=" + Arrays.toString(newMerkleValue) +
            ", partialKey=" + partialKey +
            ", childrenMerkleValues=" + childrenMerkleValues +
            '}';
    }
}

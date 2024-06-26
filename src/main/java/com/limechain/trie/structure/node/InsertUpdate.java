package com.limechain.trie.structure.node;

import com.limechain.trie.structure.nibble.Nibbles;
import jakarta.annotation.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public record InsertUpdate(byte[] newMerkleValue, List<byte[]> childrenMerkleValues, Nibbles partialKey,
                           @Nullable byte[] storageValue) implements TrieNodeChange {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        InsertUpdate that = (InsertUpdate) o;
        return Objects.equals(partialKey, that.partialKey)
            && Objects.deepEquals(storageValue, that.storageValue)
            && Objects.deepEquals(newMerkleValue, that.newMerkleValue)
            && Objects.equals(childrenMerkleValues, that.childrenMerkleValues);
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(newMerkleValue), childrenMerkleValues, partialKey, Arrays.hashCode(storageValue));
    }

    @Override
    public String toString() {
        return "InsertUpdate{" +
            "newMerkleValue=" + Arrays.toString(newMerkleValue) +
            ", childrenMerkleValues=" + childrenMerkleValues +
            ", partialKey=" + partialKey +
            ", storageValue=" + Arrays.toString(storageValue) +
            '}';
    }
}

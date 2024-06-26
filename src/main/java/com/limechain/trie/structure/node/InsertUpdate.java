package com.limechain.trie.structure.node;

import com.limechain.trie.structure.nibble.Nibbles;
import jakarta.annotation.Nullable;

import java.util.List;

public record InsertUpdate(byte[] newMerkleValue, List<byte[]> childrenMerkleValues, Nibbles partialKey,
                           @Nullable byte[] storageValue) implements TrieNodeChange {
}

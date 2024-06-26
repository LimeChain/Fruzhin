package com.limechain.trie.cache.node;

import com.limechain.trie.structure.nibble.Nibbles;

import java.util.List;

public record PendingInsertUpdate(byte[] newMerkleValue, Nibbles partialKey,
                                  List<byte[]> childrenMerkleValues) implements PendingTrieNodeChange {
}

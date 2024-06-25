package com.limechain.trie.cache.node;

import com.limechain.trie.structure.nibble.Nibbles;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
@Getter
public final class PendingInsertUpdate extends PendingTrieNodeChange {

    final byte[] newMerkleValue;
    final Nibbles partialKey;
    final List<byte[]> childrenMerkleValues;
}

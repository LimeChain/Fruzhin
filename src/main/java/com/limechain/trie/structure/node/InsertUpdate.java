package com.limechain.trie.structure.node;

import com.limechain.trie.structure.nibble.Nibbles;
import jakarta.annotation.Nullable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
@Getter
public final class InsertUpdate extends TrieNodeChange {

    private final byte[] newMerkleValue;
    private final List<byte[]> childrenMerkleValues;
    private final Nibbles partialKey;

    @Nullable
    private final byte[] storageValue;

}

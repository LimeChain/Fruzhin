package com.limechain.trie.dto.node;

import com.limechain.runtime.version.StateVersion;
import com.limechain.trie.structure.nibble.Nibbles;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@AllArgsConstructor
@Getter
public class TraversedNode {
    private final Nibbles fullKey;
    private final Nibbles partialKey;
    private final List<byte[]> childrenMerkleValues;
    private final StateVersion stateVersion;
    @Nullable
    private final byte[] value;
    @Setter
    @Nullable
    private TraversedNode parent;
}
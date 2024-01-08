package com.limechain.trie.structure;

import lombok.Data;

/**
 * A semantic wrapper for an integer, representing a node index within our trie structure.
 * Cannot be instantiated outside, so only the {@link TrieStructure} can generate them and provides them to the user
 * as an opaque index for nodes.
 */
@Data
public final class TrieNodeIndex {
    private final int value;

    TrieNodeIndex(int value) {
        this.value = value;
    }
}

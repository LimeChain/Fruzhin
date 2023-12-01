package com.limechain.trie.structure.node;

import com.limechain.trie.structure.nibble.Nibble;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@AllArgsConstructor
@Getter
public class InsertTrieNode {
    private final byte[] storageValue;
    private final byte[] merkleValue;
    private final List<byte[]> childrenMerkleValues;
    private final List<Nibble> partialKeyNibbles;
}

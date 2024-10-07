package com.limechain.trie.dto.node;

import com.limechain.trie.structure.database.NodeData;
import com.limechain.trie.structure.nibble.Nibbles;

public record StorageNode(Nibbles key, NodeData nodeData) {
}

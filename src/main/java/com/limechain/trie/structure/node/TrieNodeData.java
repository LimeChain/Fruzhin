package com.limechain.trie.structure.node;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;

/**
 * Represents the data associated with a node in a trie structure.
 * This class includes the node's value, a reference to the trie root,
 * and the version of the entries.
 */
@AllArgsConstructor
@EqualsAndHashCode
public class TrieNodeData {
    byte[] value;
    byte[] trieRootRef;
    byte entriesVersion;
}
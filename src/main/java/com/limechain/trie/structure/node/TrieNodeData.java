package com.limechain.trie.structure.node;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

import java.io.Serializable;
import java.util.List;

/**
 * Represents the data associated with a node in a trie structure.
 * This class includes the node's value, a reference to the trie root,
 * and the version of the entries.
 */
@AllArgsConstructor
@EqualsAndHashCode
@Getter
public class TrieNodeData implements Serializable {
    private byte[] partialKey;
    private List<byte[]> childrenMerkleValues;
    private byte[] value;
    private byte[] trieRootRef;
    private byte entriesVersion;
}

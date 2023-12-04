package com.limechain.trie.structure.node;

import com.limechain.trie.structure.nibble.Nibble;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

/**
 * Represents a node in a trie structure for insertion into a database.
 * This class encapsulates the storage value, merkle value, children's merkle values,
 * and partial key nibbles of a trie node.
 */
@AllArgsConstructor
@Getter

public class InsertTrieNode {
    /**
     * The storage value associated with this trie node.
     */
    private final byte[] storageValue;
    /**
     * The merkle value of this trie node.
     */
    private final byte[] merkleValue;
    /**
     * A list of merkle values of the children of this trie node.
     * Each entry in the list is a byte array representing the merkle value of a child node.
     */
    private final List<byte[]> childrenMerkleValues;
    /**
     * A list of nibbles representing the partial key associated with this trie node.
     */
    private final List<Nibble> partialKeyNibbles;
}

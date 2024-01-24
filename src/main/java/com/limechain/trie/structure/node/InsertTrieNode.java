package com.limechain.trie.structure.node;

import com.limechain.trie.structure.nibble.Nibble;

import java.util.List;

/**
 * Represents a node in a trie structure for insertion into a database.
 * This class encapsulates the storage value, merkle value, children's merkle values,
 * and partial key nibbles of a trie node.
 *
 * @param storageValue         The storage value associated with this trie node.
 * @param merkleValue          The merkle value of this trie node.
 * @param childrenMerkleValues A list of merkle values of the children of this trie node.
 *                             Each entry in the list is a byte array representing the merkle value of a child node.
 * @param partialKeyNibbles    A list of nibbles representing the partial key associated with this trie node.
 * @param isReferenceValue       Notes if the value stored is a reference to anothed node
 */
public record InsertTrieNode(byte[] storageValue, byte[] merkleValue, List<byte[]> childrenMerkleValues,
                             List<Nibble> partialKeyNibbles, boolean isReferenceValue) {
}
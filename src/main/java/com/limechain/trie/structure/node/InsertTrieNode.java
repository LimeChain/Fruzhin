package com.limechain.trie.structure.node;

import com.limechain.trie.structure.nibble.Nibbles;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

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
 * @param isReferenceValue     Notes if the value stored is a reference to another node
 */
public record InsertTrieNode(boolean isBranch, byte[] storageValue, byte[] merkleValue,
                             List<byte[]> childrenMerkleValues,
                             Nibbles partialKeyNibbles, boolean isReferenceValue) {

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (InsertTrieNode) obj;
        return this.isBranch == that.isBranch
               && Arrays.equals(this.storageValue, that.storageValue) &&
               Arrays.equals(this.merkleValue, that.merkleValue) &&
               Objects.equals(this.childrenMerkleValues, that.childrenMerkleValues) &&
               Objects.equals(this.partialKeyNibbles, that.partialKeyNibbles) &&
               this.isReferenceValue == that.isReferenceValue;
    }

    @Override
    public int hashCode() {
        return Objects.hash(Arrays.hashCode(storageValue), Arrays.hashCode(merkleValue), childrenMerkleValues,
                partialKeyNibbles, isReferenceValue);
    }

    @Override
    public String toString() {
        return "InsertTrieNode[" +
               "isBranch=" + isBranch + ", " +
               "storageValue=" + Arrays.toString(storageValue) + ", " +
               "merkleValue=" + Arrays.toString(merkleValue) + ", " +
               "childrenMerkleValues=" + childrenMerkleValues + ", " +
               "partialKeyNibbles=" + partialKeyNibbles + ", " +
               "isReferenceValue=" + isReferenceValue + ']';
    }
}
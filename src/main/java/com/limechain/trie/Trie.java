package com.limechain.trie;

import com.limechain.utils.HashUtils;
import io.emeraldpay.polkaj.types.Hash256;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.limechain.utils.ByteArrayUtils.commonPrefixLength;
import static com.limechain.utils.ByteArrayUtils.hasPrefix;

public class Trie {
    BigInteger generation;
    Node root;
    Map<Hash256, Trie> childTries;
    List<Hash256> deltas;

    public Trie(BigInteger generation, Node root, Map<Hash256, Trie> childTries, List<Hash256> deltas) {
        this.generation = generation;
        this.root = root;
        this.childTries = childTries;
        this.deltas = deltas;
    }

    /**
     * Creates new trie containing only a root node
     *
     * @param node - the root node
     * @return the new trie
     */
    public static Trie newTrie(Node node) {
        return new Trie(BigInteger.ZERO, node, new HashMap<>(), new ArrayList<>());
    }

    /**
     * Returns the merkle value of the node
     *
     * @param encoding - the encoded node
     * @return the merkle value of the node
     */
    public static byte[] getMerkleValueRoot(byte[] encoding) {
        return HashUtils.hashWithBlake2b(encoding);
    }

    public static byte[] retrieve(Node parent, byte[] key) {
        if (parent == null) {
            return new byte[0];
        }

        if (parent.getKind() == NodeKind.LEAF)
            return retrieveFromLeaf(parent, key);

        return retrieveFromBranch(parent, key);
    }

    public static byte[] retrieveFromLeaf(Node leaf, byte[] key) {
        if (Arrays.equals(leaf.getPartialKey(), key)) {
            return leaf.getStorageValue();
        }
        return new byte[0];
    }

    public static byte[] retrieveFromBranch(Node branch, byte[] key) {
        if (key.length == 0 || Arrays.equals(branch.getPartialKey(), key)) {
            return branch.getStorageValue();
        }
        if (branch.getPartialKey().length > key.length &&
                hasPrefix(branch.getPartialKey(), key)) {
            return new byte[0];
        }

        int commonPrefixLength = commonPrefixLength(branch.getPartialKey(), key);
        byte childIndex = key[commonPrefixLength];
        byte[] childKey = Arrays.copyOfRange(key, commonPrefixLength + 1, key.length);
        Node child = branch.getChild(childIndex);
        return retrieve(child, childKey);
    }

    /**
     * Returns the value in the node of the trie which matches its key with the key given.
     * Note: The key argument is given in little Endian format.
     *
     * @param keyLE - key to match
     * @return - value of the node
     */
    public byte[] get(byte[] keyLE) {
        byte[] keyNibbles = Nibbles.keyLEToNibbles(keyLE);
        return retrieve(this.root, keyNibbles);
    }

    @Override
    public String toString() {
        return "Trie{" +
                "generation=" + generation +
                ", root=" + root +
                ", childTries=" + childTries +
                ", deltas=" + deltas +
                '}';
    }
}

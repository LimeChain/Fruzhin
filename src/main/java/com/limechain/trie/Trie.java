package com.limechain.trie;

import com.limechain.utils.HashUtils;
import io.emeraldpay.polkaj.types.Hash256;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Trie {
    BigInteger generation;
    Node root;
    Map<Hash256, Trie> childTries;
    ArrayList<Hash256> deltas;

    public Trie(BigInteger generation, Node root, Map<Hash256, Trie> childTries, ArrayList<Hash256> deltas) {
        this.generation = generation;
        this.root = root;
        this.childTries = childTries;
        this.deltas = deltas;
    }

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

package com.limechain.internal;

import com.limechain.utils.HashUtils;
import io.emeraldpay.polkaj.types.Hash256;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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

    public static void hashEncoding(byte[] encoding, ByteBuffer writer) throws IOException {
        try {
            // Algorithm might be different in golang
            MessageDigest hasher = MessageDigest.getInstance("SHA-256");
            hasher.reset();

            hasher.update(encoding);
            byte[] digest = hasher.digest();

            writer.put(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Hashing algorithm not found: " + e.getMessage(), e);
        }
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

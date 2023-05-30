package com.limechain.internal;

import io.emeraldpay.polkaj.types.Hash256;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

public class Trie {
    BigInteger generation;
    Trie root;
    Map<Hash256, Trie> children;
    Vector<Hash256> deltas;

    public Trie(BigInteger generation, Trie root, Map<Hash256, Trie> children, Vector<Hash256> deltas) {
        this.generation = generation;
        this.root = root;
        this.children = children;
        this.deltas = deltas;
    }

    public static Trie newTrie() {
        return new Trie(BigInteger.ZERO, null, new HashMap<Hash256, Trie>(), new Vector<Hash256>());
    }

    public static void getMerkleValueRoot(byte[] encodedProof, ByteBuffer writer) throws IOException {
        hashEncoding(encodedProof, writer);
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
}

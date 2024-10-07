package com.limechain.trie.decoded;

import com.limechain.trie.decoded.decoder.TrieDecoder;
import com.limechain.utils.HashUtils;
import lombok.experimental.UtilityClass;
import lombok.extern.java.Log;
import org.apache.tomcat.util.buf.HexUtils;
import org.bouncycastle.util.Arrays;

import java.util.HashMap;
import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * This class is used to verify a given key and value belongs to the trie by recreating it
 * <p>
 * Inspired by Gossamer’s implementation approach
 */
@Log
@UtilityClass
public class TrieVerifier {
    public static final int MAX_PARTIAL_KEY_LENGTH = 65535;

    /**
     * Verify verifies a given key and value belongs to the trie by creating
     * a proof trie based on the encoded proof nodes given.
     *
     * @param proofTrie the partial proof tire
     * @param key       the key of the KVP to verify
     * @param value     the value of the KVP to verify
     * @return true if the (key, value) KVP is present in the provided storage, false otherwise
     */
    public static boolean verify(Trie proofTrie, byte[] key, byte[] value) {
        byte[] proofTrieValue = proofTrie.get(key);
        if (java.util.Arrays.equals(proofTrieValue, new byte[0])) {
            throw new IllegalStateException("Key not found in proof trie hash");
        }
        if (value.length > 0 && !Arrays.areEqual(value, proofTrieValue)) {
            throw new IllegalStateException("Value mismatch\nExpected: " + value + "\nActual: " + proofTrieValue);
        }
        return true;
    }

    /**
     * Builds trie based on the proof slice of encoded nodes using the Blake2-256 hash function.
     *
     * @param encodedProofNodes two-dimensional array containing the encoded proof nodes
     * @param rootHash          to search for in the proofs (hashed with Blake2-256)
     * @return a new trie with the searched root hash
     */
    public static Trie buildTrie(byte[][] encodedProofNodes, byte[] rootHash) {
        return buildTrie(encodedProofNodes, rootHash, HashUtils::hashWithBlake2b);
    }

    /**
     * Sets a partial trie based on the proof slice of encoded nodes.
     *
     * @param encodedProofNodes two-dimensional array containing the encoded proof nodes
     * @param rootHash          to search for in the proofs
     * @param hashFunction      the hash function
     * @return a new trie with the searched root hash
     */
    public static Trie buildTrie(byte[][] encodedProofNodes, byte[] rootHash, UnaryOperator<byte[]> hashFunction) {
        if (encodedProofNodes.length == 0) {
            throw new IllegalArgumentException("Encoded proof nodes is empty!");
        }

        Map<String, byte[]> digestToEncoding = new HashMap<>(encodedProofNodes.length);

        Node root = null;

        for (byte[] encodedProofNode : encodedProofNodes) {
            byte[] digest = hashFunction.apply(encodedProofNode);
            // root node already found or the hash doesn't match the root hash.
            if (root != null || !Arrays.areEqual(digest, rootHash)) {
                digestToEncoding.put(HexUtils.toHexString(digest), encodedProofNode);
                continue;
            }

            root = TrieDecoder.decode(encodedProofNode);
            root.setDirty(true);
        }

        if (root == null) {
            throw new IllegalStateException("Root node not found in proof for root hash: " +
                    HexUtils.toHexString(rootHash));
        }

        TrieProofLoader.loadProof(digestToEncoding, root);

        return Trie.newTrie(root);
    }
}
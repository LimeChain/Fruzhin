package com.limechain.trie;

import com.limechain.trie.decoder.TrieDecoder;
import com.limechain.trie.decoder.TrieDecoderException;
import lombok.extern.java.Log;
import org.apache.tomcat.util.buf.HexUtils;
import org.bouncycastle.util.Arrays;

import java.util.HashMap;
import java.util.Map;

@Log
public class TrieVerifier {
    public static final int MAX_PARTIAL_KEY_LENGTH = 65535;

    /**
     * Verify verifies a given key and value belongs to the trie by creating
     * a proof trie based on the encoded proof nodes given. The order of proofs is ignored.
     * A nil error is returned on success.
     * https://github.com/ComposableFi/ibc-go/blob/6d62edaa1a3cb0768c430dab81bb195e0b0c72db/modules/light-clients/11-beefy/types/client_state.go#L78.
     *
     * @param encodedProofNodes two-dimensional array containing the encoded proof nodes
     * @param rootHash          to search for in the proofs
     * @return a new trie with the searched root hash
         * @throws TrieDecoderException
     */
    public static boolean verify(byte[][] encodedProofNodes, byte[] rootHash, byte[] key, byte[] value) throws TrieDecoderException {
        Trie proofTrie = buildTrie(encodedProofNodes, rootHash);
        byte[] proofTrieValue = proofTrie.get(key);
        if (proofTrieValue == null) {
            throw new IllegalStateException("Key not found in proof trie hash");
        }
        if (value.length > 0 && !Arrays.areEqual(value, proofTrieValue)) {
            throw new IllegalStateException("Value mismatch\nExpected: " + value + "\nActual: " + proofTrieValue);
        }
        return true;
    }

    /**
     * Sets a partial trie based on the proof slice of encoded nodes.
     *
     * @param encodedProofNodes two-dimensional array containing the encoded proof nodes
     * @param rootHash          to search for in the proofs
     * @return a new trie with the searched root hash
     * @throws TrieDecoderException
     */
    public static Trie buildTrie(byte[][] encodedProofNodes, byte[] rootHash) throws TrieDecoderException {
        if (encodedProofNodes.length == 0) {
            throw new IllegalArgumentException("Encoded proof nodes is empty!");
        }

        Map<String, byte[]> digestToEncoding = new HashMap<>(encodedProofNodes.length);

        Node root = null;

        for (byte[] encodedProofNode : encodedProofNodes) {
            byte[] digest = Trie.getMerkleValueRoot(encodedProofNode);
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


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
     * Verifies that a given key and value belongs to the trie by creating
     * a proof trie based on the encoded proof nodes given
     *
     * @param encodedProofNodes
     * @param rootHash
     * @return
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


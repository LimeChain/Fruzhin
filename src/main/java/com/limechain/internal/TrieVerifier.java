package com.limechain.internal;

import io.emeraldpay.polkaj.types.Hash256;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class TrieVerifier {

    public boolean verify(byte[][] encodedProofNodes, Hash256 rootHash, byte[] value) {

        return true;
    }

    public Trie buildTrie(byte[][] encodedProofNodes, Hash256 rootHash) {
        if (encodedProofNodes.length == 0) {
            throw new IllegalArgumentException("Empty proof for Merkle root hash: " + rootHash);
        }

        Map<String, byte[]> digestToEncoding = new HashMap<>(encodedProofNodes.length);

        ByteBuffer buffer = ByteBuffer.allocate(encodedProofNodes.length);
        try {
            Trie root = null;
            for (byte[] encodedProofNode : encodedProofNodes) {
                buffer.clear();
                try {
                    Trie.getMerkleValueRoot(encodedProofNode, buffer);
                } catch (Exception e) {
                    throw new IllegalStateException("Cannot build trie");
                }
                byte[] digest = buffer.array();

                if (root != null || !Arrays.equals(digest, rootHash.getBytes())) {
                    digestToEncoding.put(Arrays.toString(digest), encodedProofNode);
                    continue;
                }

                try {
                    root = Trie.decode(ByteBuffer.wrap(encodedProofNode));
                } catch (Exception e) {
                    throw new BuildTrieException("Decoding root node: ", e);
                }
                root.setDirty(true);
            }
            try {
                loadProof(digestToEncoding, root);
            } catch (Exception e) {
                throw new BuildTrieException("Loading proof: ", e);
            }

            return new Trie(root);
        } finally {
            DigestBuffers.put(buffer);
        }
        return Trie.newTrie();
    }

    public static Trie decode(ScaleReader reader) throws IOException {
        Header header = decodeHeader(reader);
        int variant = header.getVariant();
        int partialKeyLength = header.getPartialKeyLength();

        switch (variant) {
            case LeafVariant.BITS:
                try {
                    return decodeLeaf(reader, partialKeyLength);
                } catch (IllegalStateException e) {
                    throw new IllegalStateException("Cannot decode leaf: ", e);
                }
            case BranchVariant.BITS:
            case BranchWithValueVariant.BITS:
                try {
                    return decodeBranch(reader, variant, partialKeyLength);
                } catch (IllegalStateException e) {
                    throw new IllegalStateException("Cannot decode branch: ", e);
                }
            default:
                throw new RuntimeException(String.format("Not implemented for node variant %08b", variant));
        }
    }
}
}

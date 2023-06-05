package com.limechain.internal;

import com.limechain.internal.tree.decoder.TreeDecoder;
import com.limechain.internal.tree.decoder.TrieDecoderException;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import lombok.extern.java.Log;
import org.apache.tomcat.util.buf.HexUtils;
import org.bouncycastle.util.Arrays;

import java.util.HashMap;
import java.util.Map;

@Log
public class TrieVerifier {
    public static final int MAX_PARTIAL_KEY_LENGTH = 65535;

    public static Trie buildTrie(byte[][] encodedProofNodes, byte[] rootHash) throws TrieDecoderException {
        if (encodedProofNodes.length == 0) {
            throw new IllegalStateException("Encoded proof nodes is empty!");
        }

        Map<String, byte[]> digestToEncoding = new HashMap<>(encodedProofNodes.length);

        Node root = null;

        for (byte[] encodedProofNode : encodedProofNodes) {
            byte[] digest = Node.getMerkleValueRoot(encodedProofNode);
            if (root != null || Arrays.areEqual(digest, rootHash)) {
                // root node already found or the hash doesn't match the root hash.
                digestToEncoding.put(HexUtils.toHexString(digest), encodedProofNode);
                continue;
            }

            root = TreeDecoder.decode(new ScaleCodecReader(encodedProofNode));
            root.setDirty(true);
        }

        if (root == null) {
            throw new IllegalStateException("Root node not found in proof for root hash: " + HexUtils.toHexString(rootHash));
        }

        loadProof(digestToEncoding, root);

        return Trie.newTrie(root);
    }

    // loadProof is a recursive function that will create all the trie paths based
    // on the map from node hash digest to node encoding, starting from the node `n`.
    private static void loadProof(Map<String, byte[]> digestToEncoding, Node n) throws TrieDecoderException {
        if (n.getKind() != NodeKind.Branch) {
            return;
        }

        // Node is a branch
        for (int i = 0; i < n.getChildren().length; i++) {
            Node child = n.getChildren()[i];
            if (child == null) {
                continue;
            }

            byte[] merkleValue = child.getMerkleValue();
            boolean keyExists = digestToEncoding.containsKey(HexUtils.toHexString(merkleValue));
            if (keyExists) {
                boolean inlinedChild = child.getStorageValue().length > 0 || child.hasChild();
                if (inlinedChild) {
                    // The built proof trie is not used with a database, but just in case
                    // it becomes used with a database in the future, we set the dirty flag
                    // to true.
                    child.setDirty(true);
                } else {
                    // hash not found and the child is not inlined,
                    // so clear the child from the branch.
                    n.setDescendants(n.getDescendants() - 1 - child.getDescendants());
                    n.getChildren()[i] = null;
                    if (!n.hasChild()) {
                        // Convert branch to a leaf if all its children are null.
                        n.setChildren(null);
                    }
                }
                continue;
            }

            byte[] encoding = digestToEncoding.get(HexUtils.toHexString(merkleValue));
            Node decodedChild = TreeDecoder.decode(new ScaleCodecReader(encoding));
            if (decodedChild == null) {
                throw new RuntimeException("Decoding child node for hash digest: " + HexUtils.toHexString(merkleValue));
            }

            // The built proof trie is not used with a database, but just in case
            // it becomes used with a database in the future, we set the dirty flag
            // to true.
            decodedChild.setDirty(true);

            Node[] children = n.getChildren();
            children[i] = decodedChild;
            n.setChildren(children);
            n.setDescendants(n.getDescendants() + decodedChild.getDescendants());
            loadProof(digestToEncoding, decodedChild);
        }
    }
}


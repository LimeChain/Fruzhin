package com.limechain.trie;

import com.limechain.trie.decoder.TrieDecoder;
import com.limechain.trie.decoder.TrieDecoderException;
import org.apache.tomcat.util.buf.HexUtils;

import java.util.Map;

public class TrieProofLoader {
    /**
     * loadProof is a recursive function that will create all the trie paths based
     * on the map from node hash digest to node encoding, starting from the node `n`.
     *
     * @param digestToEncoding - map containing hashed node digest as keys and node encodings as values
     * @param node             - storing the loaded information
     * @throws TrieDecoderException when child could not be decoded
     */
    public static void loadProof(Map<String, byte[]> digestToEncoding, Node node) throws TrieDecoderException {
        if (node.getKind() != NodeKind.Branch) {
            return;
        }

        // Node is a branch
        for (int i = 0; i < Node.CHILDREN_CAPACITY; i++) {
            Node child = node.getChild(i);
            if (child == null) {
                continue;
            }

            byte[] merkleValue = child.getMerkleValue();
            String merkleValueKey = HexUtils.toHexString(merkleValue);
            boolean keyExists = digestToEncoding.containsKey(merkleValueKey);
            if (!keyExists) {
                boolean inlinedChild = child.getStorageValueLength() > 0 || child.hasChild();
                if (inlinedChild) {
                    // The built proof trie is not used with a database, but just in case
                    // it becomes used with a database in the future, we set the dirty flag
                    // to true.
                    child.setDirty(true);
                } else {
                    // hash not found and the child is not inlined,
                    // so clear the child from the branch.
                    node.setDescendants(node.getDescendants() - 1 - child.getDescendants());
                    node.getChildren()[i] = null;
                    if (!node.hasChild()) {
                        // Convert branch to a leaf if all its children are null.
                        node.setChildren(null);
                    }
                }
                continue;
            }

            byte[] encoding = digestToEncoding.get(merkleValueKey);
            Node decodedChild = TrieDecoder.decode(encoding);
            if (decodedChild == null) {
                throw new RuntimeException("Decoding child node for hash digest: " + HexUtils.toHexString(merkleValue));
            }

            // The built proof trie is not used with a database, but just in case
            // it becomes used with a database in the future, we set the dirty flag
            // to true.
            decodedChild.setDirty(true);

            Node[] children = node.getChildren();
            children[i] = decodedChild;
            node.setChildren(children);
            node.setDescendants(node.getDescendants() + decodedChild.getDescendants());
            loadProof(digestToEncoding, decodedChild);
        }
    }
}

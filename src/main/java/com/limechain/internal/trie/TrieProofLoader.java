package com.limechain.internal.trie;

import com.limechain.internal.Node;
import com.limechain.internal.NodeKind;
import com.limechain.internal.tree.decoder.TreeDecoder;
import com.limechain.internal.tree.decoder.TrieDecoderException;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import org.apache.tomcat.util.buf.HexUtils;

import java.util.Map;

public class TrieProofLoader {
    // loadProof is a recursive function that will create all the trie paths based
    // on the map from node hash digest to node encoding, starting from the node `n`.
    public static void loadProof(Map<String, byte[]> digestToEncoding, Node n) throws TrieDecoderException {
        if (n.getKind() != NodeKind.Branch) {
            return;
        }

        // Node is a branch
        for (int i = 0; i < Node.CHILDREN_CAPACITY; i++) {
            Node child = n.getChild(i);
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
                    n.setDescendants(n.getDescendants() - 1 - child.getDescendants());
                    n.getChildren()[i] = null;
                    if (!n.hasChild()) {
                        // Convert branch to a leaf if all its children are null.
                        n.setChildren(null);
                    }
                }
                continue;
            }

            byte[] encoding = digestToEncoding.get(merkleValueKey);
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

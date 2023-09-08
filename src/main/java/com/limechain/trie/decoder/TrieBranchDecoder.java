package com.limechain.trie.decoder;

import com.limechain.trie.Node;
import com.limechain.trie.NodeVariant;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.types.Hash256;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TrieBranchDecoder {
    public static final int CHILD_BITMAP_SIZE = 2;

    /**
     * Decodes a branch node and its children recursively from a ScaleCodecReader input stream.
     *
     * @param reader           the ScaleCodecReader to read the encoded node data from
     * @param variant          the variant of the node to be decoded
     * @param partialKeyLength the length of the partial key to be read
     * @return the decoded Node object
     * @throws TrieDecoderException if an error occurs while decoding the node. This could be
     *                              due to an issue reading the children bitmap or the storage value.
     */
    public static Node decode(ScaleCodecReader reader, NodeVariant variant, int partialKeyLength) {
        Node node = new Node();
        node.setChildren(new Node[Node.CHILDREN_CAPACITY]);
        node.setPartialKey(TrieKeyDecoder.decodeKey(reader, partialKeyLength));

        byte[] childrenBitmap;
        try {
            childrenBitmap = reader.readByteArray(CHILD_BITMAP_SIZE);
        } catch (IndexOutOfBoundsException error) {
            throw new TrieDecoderException("Could not decode children bitmap: " + error.getMessage());
        }

        switch (variant) {
            case BRANCH_WITH_VALUE -> {
                try {
                    node.setStorageValue(reader.readByteArray());
                } catch (IndexOutOfBoundsException e) {
                    throw new TrieDecoderException("Could not decode storage value: " + e.getMessage());
                }
            }
            case BRANCH_WITH_HASHED_VALUE -> {
                try {
                    byte[] hashedValue = reader.readByteArray(Hash256.SIZE_BYTES);
                    node.setStorageValue(hashedValue);
                    node.setValueHashed(true);
                } catch (IndexOutOfBoundsException e) {
                    throw new TrieDecoderException("Could not decode storage value: " + e.getMessage());
                }
            }
            default -> {
                // Do nothing
            }
        }

        // Decode children
        for (int i = 0; i < Node.CHILDREN_CAPACITY; i++) {
            // Skip if the bit is not set
            if (((childrenBitmap[i / 8] >> (i % 8)) & 1) != 1) {
                continue;
            }

            try {
                byte[] hash = reader.readByteArray();
                Node child = new Node();
                child.setMerkleValue(hash);
                if (hash.length < Hash256.SIZE_BYTES) {
                    child = TrieDecoder.decode(hash);
                    if (child == null) throw new TrieDecoderException("Could not decode inlined child node");
                    node.setDescendants(node.getDescendants() + child.getDescendants());
                }
                node.setDescendants(node.getDescendants() + 1);
                node.setChildrenAt(child, i);
            } catch (IndexOutOfBoundsException e) {
                throw new TrieDecoderException("Could not decode child hash: " + e.getMessage());
            }

        }

        return node;
    }
}

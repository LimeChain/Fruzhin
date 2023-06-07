package com.limechain.trie.decoder;

import com.limechain.trie.Node;
import com.limechain.trie.NodeVariant;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.types.Hash256;

public class TrieBranchDecoder {
    /**
     * Decodes a branch node from a ScaleCodecReader input stream.
     *
     * @param reader the ScaleCodecReader to read the encoded node data from
     * @param variantByte the variant byte that represents the node variant
     * @param partialKeyLength the length of the partial key to be read
     * @return the decoded Node object
     * @throws TrieDecoderException if an error occurs while decoding the node. This could be
     * due to an issue reading the children bitmap or the storage value.
     */
    public static Node decode(ScaleCodecReader reader, byte variantByte, int partialKeyLength)
            throws TrieDecoderException {
        Node node = new Node();
        node.setChildren(new Node[Node.CHILDREN_CAPACITY]);
        node.setPartialKey(TrieKeyDecoder.decodeKey(reader, partialKeyLength));

        byte[] childrenBitmap;
        try {
            childrenBitmap = reader.readByteArray(2);
        } catch (IndexOutOfBoundsException error) {
            throw new TrieDecoderException("Could not decode children bitmap: " + error.getMessage());
        }

        int variant = variantByte & 0xff;
        if (variant == NodeVariant.BRANCH_WITH_VALUE.bits) {
            try {
                node.setStorageValue(reader.readByteArray());
            } catch (IndexOutOfBoundsException e) {
                throw new TrieDecoderException("Could not decode storage value: " + e.getMessage());
            }
        }

        decodeChildren(node, childrenBitmap, reader);
        return node;
    }

    /**
     * Decodes the children of a given Node from a ScaleCodecReader input stream and sets them to the node
     *
     * @param node the Node object whose children are to be decoded
     * @param childrenBitmap the bitmap representing the presence of children in the node
     * @param reader the ScaleCodecReader to read the encoded child node data from
     * @throws TrieDecoderException if an error occurs while decoding the child node, e.g.
     * due to an issue reading the child's hash value.
     */
    private static void decodeChildren(Node node, byte[] childrenBitmap, ScaleCodecReader reader)
            throws TrieDecoderException {
        for (int i = 0; i < Node.CHILDREN_CAPACITY; i++) {
            // Skip if the bit is not set
            if (((childrenBitmap[i / 8] >> (i % 8)) & 1) != 1) {
                continue;
            }
            byte[] hash;
            try {
                hash = reader.readByteArray();
            } catch (IndexOutOfBoundsException e) {
                throw new TrieDecoderException("Could not decode child hash: " + e.getMessage());
            }
            Node child = new Node();
            child.setMerkleValue(hash);
            if (hash.length < Hash256.SIZE_BYTES) {
                child = TrieDecoder.decode(hash);
                node.setDescendants(node.getDescendants() + child.getDescendants());
            }
            node.setDescendants(node.getDescendants() + 1);
            node.setChildrenAt(child, i);
        }
    }
}

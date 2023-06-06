package com.limechain.internal.tree.decoder;

import com.limechain.internal.Node;
import com.limechain.internal.NodeVariant;
import com.limechain.internal.Trie;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.types.Hash256;

import static com.limechain.internal.tree.decoder.TreeDecoder.decode;

public class BranchDecoder {
    public static Node decodeBranch(ScaleCodecReader reader, byte variantByte, int partialKeyLength)
            throws TrieDecoderException {
        Node node = new Node();
        node.setChildren(new Node[Node.CHILDREN_CAPACITY]);

        node.setPartialKey(HeaderDecoder.decodeKey(reader, partialKeyLength));

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

    private static void decodeChildren(Node node, byte[] childrenBitmap, ScaleCodecReader reader)
            throws TrieDecoderException {
        for (int i = 0; i < Node.CHILDREN_CAPACITY; i++) {
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
                ScaleCodecReader inlinedChildReader = new ScaleCodecReader(hash);
                child = decode(inlinedChildReader);
                node.setDescendants(node.getDescendants() + child.getDescendants());
            }
            node.setDescendants(node.getDescendants() + 1);
            node.setChildrenAt(child, i);
        }
    }
}

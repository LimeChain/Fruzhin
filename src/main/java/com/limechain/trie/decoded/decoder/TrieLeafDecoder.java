package com.limechain.trie.decoded.decoder;

import com.limechain.trie.decoded.Node;
import com.limechain.trie.decoded.NodeVariant;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;
import io.emeraldpay.polkaj.types.Hash256;
import lombok.experimental.UtilityClass;

/**
 * This class is used to decode leaf nodes from a ScaleCodecReader input stream.
 */
@UtilityClass
public class TrieLeafDecoder {
    /**
     * Decodes a leaf or hashed leaf node from a ScaleCodecReader input stream.
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
        node.setPartialKey(TrieKeyDecoder.decodeKey(reader, partialKeyLength));

        // Hashed leaf node
        if (variant == NodeVariant.LEAF_WITH_HASHED_VALUE) {
            try {
                byte[] hashedValue = reader.readByteArray(Hash256.SIZE_BYTES);
                node.setStorageValue(hashedValue);
                node.setValueHashed(true);
                return node;
            } catch (IndexOutOfBoundsException e) {
                throw new TrieDecoderException("Could not decode hashed storage value: " + e.getMessage());
            }
        }

        // Normal leaf node
        try {
            byte[] storageValue = reader.readByteArray();
            node.setStorageValue(storageValue);
        } catch (IndexOutOfBoundsException | UnsupportedOperationException e) {
            throw new TrieDecoderException("Could not decode storage value: " + e.getMessage());
        }
        return node;
    }
}

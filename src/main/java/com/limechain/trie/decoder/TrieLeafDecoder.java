package com.limechain.trie.decoder;

import com.limechain.trie.Node;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;

public class TrieLeafDecoder {
    /**
     * Decodes a leaf node from a ScaleCodecReader input stream.
     *
     * @param reader the ScaleCodecReader to read the encoded node data from
     * @param partialKeyLength the length of the partial key to be read
     * @return the decoded Node object
     * @throws TrieDecoderException if an error occurs while decoding the node. This could be
     * due to an issue reading the children bitmap or the storage value.
     */
    public static Node decode(ScaleCodecReader reader, int partialKeyLength) {
        Node node = new Node();
        node.setPartialKey(TrieKeyDecoder.decodeKey(reader, partialKeyLength));

        // Decode storage:
        // https://spec.polkadot.network/sect-metadata#defn-rtm-storage-entry-type
        try {
            node.setStorageValue(reader.readByteArray());
        } catch (IndexOutOfBoundsException | UnsupportedOperationException e) {
            throw new TrieDecoderException("Could not decode storage value: " + e.getMessage());
        }
        return node;
    }
}

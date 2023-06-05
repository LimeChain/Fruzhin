package com.limechain.internal.tree.decoder;

import com.limechain.internal.Node;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;

public class LeafDecoder {
    public static Node decodeLeaf(ScaleCodecReader reader, int partialKeyLength) throws TrieDecoderException {
        Node node = new Node();
        node.setPartialKey(HeaderDecoder.decodeKey(reader, partialKeyLength));

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

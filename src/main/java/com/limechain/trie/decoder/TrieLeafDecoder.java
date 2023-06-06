package com.limechain.trie.decoder;

import com.limechain.trie.Node;
import io.emeraldpay.polkaj.scale.ScaleCodecReader;

public class TrieLeafDecoder {
    public static Node decode(ScaleCodecReader reader, int partialKeyLength) throws TrieDecoderException {
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

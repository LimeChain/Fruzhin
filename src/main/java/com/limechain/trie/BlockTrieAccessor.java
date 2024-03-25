package com.limechain.trie;

import com.limechain.storage.block.BlockState;
import com.limechain.trie.structure.NodeHandle;
import com.limechain.trie.structure.database.NodeData;
import com.limechain.trie.structure.nibble.Nibbles;
import io.emeraldpay.polkaj.types.Hash256;

import java.util.Optional;

public class BlockTrieAccessor extends TrieAccessor {

    public BlockTrieAccessor(Hash256 blockHash) {
        super(null);

        if (BlockState.getInstance().isInitialized()) {
            super.lastRoot = BlockState.getInstance().getHeader(blockHash).getStateRoot().getBytes();
        }
    }

    public Optional<byte[]> findMerkleValue(Nibbles key) {
        loadPathToKey(key);
        return partialTrie.existingNode(key)
                .map(NodeHandle::getUserData)
                .map(NodeData::getMerkleValue);
    }

}

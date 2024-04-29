package com.limechain.trie;

import com.limechain.storage.block.BlockState;
import io.emeraldpay.polkaj.types.Hash256;

/**
 * BlockTrieAccessor provides access to the trie structure of a specific block.
 * It extends TrieAccessor and inherits its functionalities for key-value storage and retrieval.
 */
public class BlockTrieAccessor extends TrieAccessor {

    public BlockTrieAccessor(Hash256 blockHash) {
        super(blockHash.getBytes());

        BlockState blockState = BlockState.getInstance();
        if (blockState.isInitialized() && blockState.hasHeader(blockHash)) {
            super.lastRoot = blockState.getHeader(blockHash).getStateRoot().getBytes();
        }
    }

    public BlockTrieAccessor(byte[] lastRoot) {
        super(lastRoot);
    }

}

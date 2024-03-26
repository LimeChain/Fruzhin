package com.limechain.trie;

import com.limechain.storage.block.BlockState;
import io.emeraldpay.polkaj.types.Hash256;

public class BlockTrieAccessor extends TrieAccessor {

    public BlockTrieAccessor(Hash256 blockHash) {
        super(null);

        BlockState blockState = BlockState.getInstance();
        if (blockState.isInitialized() && blockState.hasHeader(blockHash)) {
            super.lastRoot = blockState.getHeader(blockHash).getStateRoot().getBytes();
        }
    }

}

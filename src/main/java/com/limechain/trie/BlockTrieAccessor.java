package com.limechain.trie;

import com.limechain.storage.block.BlockState;
import io.emeraldpay.polkaj.types.Hash256;

public class BlockTrieAccessor extends TrieAccessor {

    public BlockTrieAccessor(Hash256 blockHash) {
        super(null);

        if (BlockState.getInstance().isInitialized()) {
            super.lastRoot = BlockState.getInstance().getHeader(blockHash).getStateRoot().getBytes();
        }
    }

}

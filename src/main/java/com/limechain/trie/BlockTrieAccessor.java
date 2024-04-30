package com.limechain.trie;

import com.limechain.storage.block.BlockState;
import io.emeraldpay.polkaj.types.Hash256;

/**
 * BlockTrieAccessor provides access to the trie structure of a specific block.
 * It extends TrieAccessor and inherits its functionalities for key-value storage and retrieval.
 */
public class BlockTrieAccessor extends TrieAccessor {

    /**
     * Constructs a new BlockTrieAccessor using a {@link Hash256} object representing the state root hash.
     *
     * @param stateRootHash the state root hash of the block whose trie is to be accessed
     */
    public BlockTrieAccessor(Hash256 stateRootHash) {
        super(stateRootHash.getBytes());

        BlockState blockState = BlockState.getInstance();
        if (blockState.isInitialized() && blockState.hasHeader(stateRootHash)) {
            super.lastRoot = blockState.getHeader(stateRootHash).getStateRoot().getBytes();
        }
    }

    public BlockTrieAccessor(byte[] lastRoot) {
        super(lastRoot);
    }

}

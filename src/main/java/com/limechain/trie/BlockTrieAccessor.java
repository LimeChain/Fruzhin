package com.limechain.trie;

import com.limechain.storage.block.BlockState;
import com.limechain.storage.trie.TrieStorage;
import io.emeraldpay.polkaj.types.Hash256;

/**
 * BlockTrieAccessor provides access to the trie structure of a specific block.
 * It extends TrieAccessor and inherits its functionalities for key-value storage and retrieval.
 */
public class BlockTrieAccessor extends TrieAccessor {

    /**
     * Constructs a new BlockTrieAccessor using a {@link Hash256} block hash.
     *
     * @param blockHash the block hash of the block whose trie is to be accessed
     */
    public BlockTrieAccessor(TrieStorage trieStorage, Hash256 blockHash) {
        super(trieStorage, BlockState.getInstance().isInitialized() ?
                BlockState.getInstance().getHeader(blockHash).getStateRoot().getBytes() :
                null);
    }

    public BlockTrieAccessor(TrieStorage trieStorage, byte[] stateRoot) {
        super(trieStorage, stateRoot);
    }

}

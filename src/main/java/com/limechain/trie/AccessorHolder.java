package com.limechain.trie;

import lombok.Getter;
import lombok.Setter;

/**
 * AccessorHolder is a singleton class responsible for managing instances of BlockTrieAccessor.
 * It provides methods to set the current BlockTrieAccessor instance to either the genesis block or a specific state root.
 */
@Getter
@Setter
public class AccessorHolder {
    private static AccessorHolder instance;

    private BlockTrieAccessor blockTrieAccessor;

    private AccessorHolder() { }

    public static AccessorHolder getInstance() {
        if (instance == null) {
            instance = new AccessorHolder();
        }
        return instance;
    }

    /**
     * Sets the current BlockTrieAccessor instance to the trie structure with the specified last root hash.
     *
     * @param lastRoot The last root hash of the block trie.
     * @return         The BlockTrieAccessor instance representing the block trie with the specified root hash.
     */
    public BlockTrieAccessor setToStateRoot(byte[] lastRoot) {
        this.blockTrieAccessor = new BlockTrieAccessor(lastRoot);
        return this.blockTrieAccessor;
    }

}

package com.limechain.trie;

import io.emeraldpay.polkaj.types.Hash256;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccessorHolder {
    private BlockTrieAccessor blockTrieAccessor;

    private static AccessorHolder instance;

    private AccessorHolder() { }

    public static AccessorHolder getInstance() {
        if (instance == null) {
            instance = new AccessorHolder();
        }
        return instance;
    }

    public BlockTrieAccessor setToBlock(Hash256 blockHash) {
        this.blockTrieAccessor = new BlockTrieAccessor(blockHash);
        return this.blockTrieAccessor;
    }
}

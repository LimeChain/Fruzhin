package com.limechain.trie;

import com.limechain.constants.GenesisBlockHash;
import com.limechain.rpc.server.AppBean;
import io.emeraldpay.polkaj.types.Hash256;
import lombok.Getter;
import lombok.Setter;

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

    public BlockTrieAccessor setToGenesis() {
        Hash256 genesisHash = AppBean.getBean(GenesisBlockHash.class).getGenesisHash();
        this.blockTrieAccessor = new BlockTrieAccessor(genesisHash);
        return this.blockTrieAccessor;
    }

    public BlockTrieAccessor setToStateRoot(byte[] lastRoot) {
        this.blockTrieAccessor = new BlockTrieAccessor(lastRoot);
        return this.blockTrieAccessor;
    }
}

package com.limechain.network.protocol.warp.dto;

import com.limechain.polkaj.Hash256;
import lombok.Getter;

public class WarpSyncRequest {
    @Getter
    private final Hash256 blockHash;

    public WarpSyncRequest(String blockHash) {
        this.blockHash = Hash256.from(blockHash);
    }
}

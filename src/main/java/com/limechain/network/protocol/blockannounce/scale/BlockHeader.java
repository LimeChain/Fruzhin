package com.limechain.network.protocol.blockannounce.scale;

import io.emeraldpay.polkaj.types.Hash256;

public class BlockHeader {
    public Hash256 parentHash;
    public long blockNumber;
    public Hash256 stateRoot;
    public Hash256 extrinsicRoot;
    public String digest;

    @Override
    public String toString() {
        return "BlockHeader{" +
                "parentHash=" + parentHash +
                ", blockNumber=" + blockNumber +
                ", stateRoot=" + stateRoot +
                ", extrinsicRoot=" + extrinsicRoot +
                ", digest='" + digest + '\'' +
                '}';
    }
}

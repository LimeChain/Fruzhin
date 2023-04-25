package com.limechain.network.protocol.warp.dto;

import io.emeraldpay.polkaj.types.Hash256;
import lombok.Getter;
import lombok.Setter;

import java.math.BigInteger;
import java.util.Arrays;

@Setter
@Getter
public class BlockHeader {
    private Hash256 parentHash;
    private BigInteger blockNumber;
    private Hash256 stateRoot;
    private Hash256 extrinsicsRoot;
    private HeaderDigest[] digest;

    @Override
    public String toString() {
        return "BlockHeader{" +
                "parentHash=" + parentHash +
                ", blockNumber=" + blockNumber +
                ", stateRoot=" + stateRoot +
                ", extrinsicsRoot=" + extrinsicsRoot +
                ", digest=" + Arrays.toString(digest) +
                '}';
    }
}

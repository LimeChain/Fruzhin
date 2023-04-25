package com.limechain.network.protocol.warp.dto;

import io.emeraldpay.polkaj.types.Hash256;
import lombok.Setter;

import java.math.BigInteger;
import java.util.Arrays;

@Setter
public class WarpSyncJustification {
    public BigInteger round;
    public Hash256 targetHash;
    public BigInteger targetBlock;
    public Precommit[] precommits;
    public BlockHeader[] ancestryVotes;

    @Override
    public String toString() {
        return "WarpSyncJustification{" +
                "round=" + round +
                ", targetHash=" + targetHash +
                ", targetBlock=" + targetBlock +
                ", precommits=" + Arrays.toString(precommits) +
                ", ancestryVotes=" + Arrays.toString(ancestryVotes) +
                '}';
    }
}

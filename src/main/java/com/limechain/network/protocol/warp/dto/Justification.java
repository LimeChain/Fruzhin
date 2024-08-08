package com.limechain.network.protocol.warp.dto;

import com.limechain.polkaj.Hash256;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;

import java.math.BigInteger;
import java.util.Arrays;

@Setter
@Getter
@Log
public class Justification {
    private BigInteger round;
    private Hash256 targetHash;
    private BigInteger targetBlock;
    private Precommit[] precommits;
    private BlockHeader[] ancestryVotes;

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

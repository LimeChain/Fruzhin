package com.limechain.sync;

import io.emeraldpay.polkaj.types.Hash256;

import java.math.BigInteger;
import java.util.Map;

public class Context {
    private Map<Hash256, VoterInfo> voters;
    private BigInteger voterWeight;
    private BigInteger totalWeight;

    public VoterInfo getVoter(Hash256 id){
        return voters.get(id);
    }

    public boolean voterExists(Hash256 id){
        return voters.containsKey(id);
    }
}

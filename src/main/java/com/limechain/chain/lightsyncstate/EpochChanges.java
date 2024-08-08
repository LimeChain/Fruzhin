package com.limechain.chain.lightsyncstate;

import com.limechain.polkaj.Hash256;
import lombok.Getter;
import lombok.Setter;
import com.limechain.tuple.Pair;

import java.math.BigInteger;
import java.util.Map;

@Getter
@Setter
public class EpochChanges {
    private ForkTree<PersistedEpochHeader> inner;

    private Map<Pair<Hash256, BigInteger>, PersistedEpoch> epochs;
}

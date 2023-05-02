package com.limechain.chain.lightsyncstate;

import io.emeraldpay.polkaj.types.Hash256;
import lombok.Getter;
import lombok.Setter;
import org.javatuples.Pair;

import java.math.BigInteger;
import java.util.TreeMap;

@Getter
@Setter
public class EpochChanges {
    private ForkTree<PersistedEpochHeader> inner;

    // We can use this library to store the epochs if TreeMap is not performant enough:
    // https://github.com/batterseapower/btreemap
    private TreeMap<Pair<Hash256, BigInteger>, PersistedEpoch> epochs;

}

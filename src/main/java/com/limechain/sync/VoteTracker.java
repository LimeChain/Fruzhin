package com.limechain.sync;

import io.emeraldpay.polkaj.types.Hash256;
import io.emeraldpay.polkaj.types.Hash512;
import org.javatuples.Pair;

import java.util.List;
import java.util.Map;

public class VoteTracker {
    //votes: BTreeMap<Id, VoteMultiplicity<Vote, Signature>>,
    Map<Hash256, List<Pair<Prevote, Hash512>>> votes;

}

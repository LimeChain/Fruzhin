package com.limechain.chain.lightsyncstate;

import io.emeraldpay.polkaj.types.Hash256;
import lombok.Getter;
import lombok.Setter;
import org.javatuples.Pair;

import java.math.BigInteger;

@Getter
@Setter
public class AuthoritySet {
    private Authority[] currentAuthorities;
    private BigInteger setId;
    private ForkTree<PendingChange> pendingStandardChanges;
    private PendingChange[] pendingForcedChanges;
    private Pair<BigInteger, Hash256>[] authoritySetChanges;
}

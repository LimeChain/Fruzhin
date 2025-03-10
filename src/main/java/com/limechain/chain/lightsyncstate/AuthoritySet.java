package com.limechain.chain.lightsyncstate;

import com.limechain.storage.forktree.ForkTree;
import io.emeraldpay.polkaj.types.Hash256;
import lombok.Getter;
import lombok.Setter;
import org.javatuples.Pair;

import java.math.BigInteger;

//TODO: Remove
@Getter
@Setter
public class AuthoritySet {
    private Authority[] currentAuthorities;
    private BigInteger setId;
    private ForkTree<PendingChange> pendingScheduledChanges;
    private PendingChange[] pendingForcedChanges;
    private Pair<BigInteger, Hash256>[] authoritySetChanges;
}

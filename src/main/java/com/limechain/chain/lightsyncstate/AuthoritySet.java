package com.limechain.chain.lightsyncstate;

import com.limechain.consensus.grandpa.dto.GrandpaAuthoritySet;
import com.limechain.storage.forktree.ForkTree;
import io.emeraldpay.polkaj.types.Hash256;
import lombok.Getter;
import lombok.Setter;
import org.javatuples.Pair;

import java.math.BigInteger;

@Getter
@Setter
public class AuthoritySet {

    private GrandpaAuthoritySet authoritySet;
    private ForkTree<PendingChange> pendingStandardChanges;
    private PendingChange[] pendingForcedChanges;
    private Pair<BigInteger, Hash256>[] authoritySetChanges;
}

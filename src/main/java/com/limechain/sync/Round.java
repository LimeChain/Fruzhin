package com.limechain.sync;

import com.limechain.chain.Chain;
import com.limechain.network.protocol.warp.dto.Precommit;
import io.emeraldpay.polkaj.types.Hash256;
import io.emeraldpay.polkaj.types.Hash512;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;

import java.math.BigInteger;
import java.util.logging.Level;

@Getter
@Setter
@Log
public class Round {
    BigInteger roundNumber;
    Context context;
    //    graph: VoteGraph<H, N, VoteNode>, // DAG of blocks which have been voted on.
    VoteTracker prevote;// tracks prevotes that have been counted
//    precommit: VoteTracker<Id, Precommit<H, N>, Signature>, // tracks precommits
//    historical_votes: HistoricalVotes<H, N, Signature, Id>,
//    prevote_ghost: Option<(H, N)>,   // current memoized prevote-GHOST block
//    precommit_ghost: Option<(H, N)>, // current memoized precommit-GHOST block
//    finalized: Option<(H, N)>,       // best finalized block in this round.
//    estimate: Option<(H, N)>,        // current memoized round-estimate
//    completable: bool,               // whether the round is completable

    public void importPrecommit(Chain chain/*this should probably be our data of blocks"*/,
                                Precommit precommit, Hash256 signer, Hash512 Signature) {

        boolean validVoter = true;
        if (!context.voterExists(signer)) {
            log.log(Level.WARNING, "Signer doesnt exists as voter");
            validVoter = false;
        }

        var weight = context.getVoter(signer).getWeight();

    }
}

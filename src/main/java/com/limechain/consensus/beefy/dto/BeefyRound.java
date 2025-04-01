package com.limechain.consensus.beefy.dto;

import com.limechain.network.protocol.beefy.messages.vote.BeefyVoteMessage;
import io.emeraldpay.polkaj.types.Hash264;
import lombok.Value;

import java.math.BigInteger;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Value
public class BeefyRound {
    Map<Hash264, BeefyVoteMessage> signedVotes = new ConcurrentHashMap<>();

    public boolean addVote(Hash264 authorityId, BeefyVoteMessage voteMessage) {
        return signedVotes.putIfAbsent(authorityId, voteMessage) == null;
    }

    public boolean isDone(BigInteger threshold) {
        return BigInteger.valueOf(signedVotes.size()).compareTo(threshold) >= 0;
    }
}

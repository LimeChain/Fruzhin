package com.limechain.consensus.beefy.dto;

import com.limechain.network.protocol.beefy.messages.vote.VoteMessage;
import lombok.Value;

@Value
public class DoubleVotingProof {
    VoteMessage first;
    VoteMessage second;
}

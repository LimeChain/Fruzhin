package com.limechain.consensus.beefy.dto;

import com.limechain.network.protocol.beefy.messages.vote.BeefyVoteMessage;
import lombok.Value;

@Value
public class DoubleVotingProof {
    BeefyVoteMessage first;
    BeefyVoteMessage second;
}

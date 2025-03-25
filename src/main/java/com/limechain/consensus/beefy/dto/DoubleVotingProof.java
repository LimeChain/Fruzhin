package com.limechain.consensus.beefy.dto;

import com.limechain.network.protocol.beefy.messages.vote.VoteMessage;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DoubleVotingProof {
    private VoteMessage first;
    private VoteMessage second;
}

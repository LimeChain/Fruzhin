package com.limechain.network.protocol.beefy.messages.vote;

import com.limechain.consensus.beefy.dto.Commitment;
import lombok.Value;

@Value
public class VoteMessage {
    Commitment commitment;
    byte[] authorityId;
    byte[] signature;
}

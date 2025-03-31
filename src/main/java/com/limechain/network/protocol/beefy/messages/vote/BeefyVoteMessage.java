package com.limechain.network.protocol.beefy.messages.vote;

import com.limechain.consensus.beefy.dto.Commitment;
import lombok.Value;

import java.io.Serializable;

@Value
public class BeefyVoteMessage implements Serializable {
    Commitment commitment;
    byte[] authorityId;
    byte[] signature;
}

package com.limechain.network.protocol.beefy.messages.vote;

import com.limechain.consensus.beefy.dto.Commitment;
import lombok.Data;

@Data
public class VoteMessage {
    private Commitment commitment;
    private byte[] authorityId;
    private byte[] signature;
}

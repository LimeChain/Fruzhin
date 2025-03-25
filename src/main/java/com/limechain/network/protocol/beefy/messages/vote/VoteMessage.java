package com.limechain.network.protocol.beefy.messages.vote;

import com.limechain.consensus.beefy.dto.Commitment;
import lombok.Data;

import java.io.Serializable;

@Data
public class VoteMessage implements Serializable {
    private Commitment commitment;
    private byte[] authorityId;
    private byte[] signature;
}

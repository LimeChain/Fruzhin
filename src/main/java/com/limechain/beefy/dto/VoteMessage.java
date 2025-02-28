package com.limechain.beefy.dto;

import io.libp2p.core.crypto.PubKey;
import lombok.Data;

@Data
public class VoteMessage {
    private Commitment commitment;
    private byte[] authorityId;
    private PubKey signature;
}

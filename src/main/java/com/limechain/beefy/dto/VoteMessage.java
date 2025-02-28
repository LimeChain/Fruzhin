package com.limechain.beefy.dto;

import lombok.Data;

@Data
public class VoteMessage {
    private Commitment commitment;
    private byte[] authorityId;
    private byte[] signature;
}

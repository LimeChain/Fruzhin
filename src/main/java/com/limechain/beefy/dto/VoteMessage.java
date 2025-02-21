package com.limechain.beefy.dto;

import com.limechain.runtime.hostapi.dto.Signature;
import lombok.Data;

import java.math.BigInteger;

@Data
public class VoteMessage {
    private Commitment commitment;
    private BigInteger authorityId;
    private Signature signature;
}

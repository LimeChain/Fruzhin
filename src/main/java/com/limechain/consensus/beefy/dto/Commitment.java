package com.limechain.consensus.beefy.dto;

import lombok.Value;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.List;

@Value
public class Commitment implements Serializable {
    List<PayloadElement> payload;
    BigInteger blockNumber;
    BigInteger authoritySetId;
}

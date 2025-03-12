package com.limechain.consensus.beefy.dto;

import lombok.Value;

import java.math.BigInteger;
import java.util.List;

@Value
public class Commitment {
    List<PayloadElement> payload;
    BigInteger blockNumber;
    BigInteger validatorSetId;
}

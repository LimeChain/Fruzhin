package com.limechain.beefy.dto;

import lombok.Data;

import java.math.BigInteger;
import java.util.List;

@Data
public class Commitment {
    private List<PayloadElement> payload;
    private BigInteger blockNumber;
    private BigInteger validatorSetId;
}

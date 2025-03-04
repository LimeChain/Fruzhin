package com.limechain.beefy.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Commitment {
    private List<PayloadElement> payload;
    private BigInteger blockNumber;
    private BigInteger validatorSetId;
}

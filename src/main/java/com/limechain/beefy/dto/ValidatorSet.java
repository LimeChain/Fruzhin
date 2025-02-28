package com.limechain.beefy.dto;

import lombok.Value;

import java.math.BigInteger;
import java.util.List;

@Value
public class ValidatorSet {
    List<byte[]> validators;
    BigInteger setId;
}

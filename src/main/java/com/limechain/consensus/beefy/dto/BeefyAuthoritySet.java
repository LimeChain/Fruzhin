package com.limechain.consensus.beefy.dto;

import lombok.Value;

import java.math.BigInteger;
import java.util.List;

@Value
public class BeefyAuthoritySet {

    BigInteger setId;
    List<byte[]> publicKeys;
}

package com.limechain.consensus.beefy.dto;

import lombok.Value;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.List;

@Value
public class BeefyAuthoritySet implements Serializable {

    List<byte[]> publicKeys;
    BigInteger setId;
}

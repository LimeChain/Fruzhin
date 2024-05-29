package com.limechain.chain.lightsyncstate;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;
import java.math.BigInteger;

@Getter
@AllArgsConstructor
public class Authority implements Serializable {
    private final byte[] publicKey;
    private final BigInteger weight;
}

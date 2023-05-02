package com.limechain.chain.lightsyncstate;

import io.emeraldpay.polkaj.types.Hash256;
import lombok.Getter;
import lombok.Setter;

import java.math.BigInteger;

@Getter
@Setter
public class Authority {
    private Hash256 publicKey;
    private BigInteger weight;
}

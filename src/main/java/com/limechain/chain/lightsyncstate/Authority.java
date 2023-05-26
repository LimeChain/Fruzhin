package com.limechain.chain.lightsyncstate;

import io.emeraldpay.polkaj.types.Hash256;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigInteger;

@Getter
@Setter
@NoArgsConstructor
public class Authority {
    private Hash256 publicKey;
    private BigInteger weight;

    public Authority(Hash256 publicKey, BigInteger weight) {
        this.publicKey = publicKey;
        this.weight = weight;
    }
}

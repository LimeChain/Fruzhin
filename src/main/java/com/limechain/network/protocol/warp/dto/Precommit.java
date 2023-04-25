package com.limechain.network.protocol.warp.dto;

import io.emeraldpay.polkaj.types.Hash256;
import io.emeraldpay.polkaj.types.Hash512;
import lombok.Getter;
import lombok.Setter;

import java.math.BigInteger;

@Setter
@Getter
public class Precommit {
    private Hash256 targetHash;
    private BigInteger targetNumber;
    private Hash512 signature;
    private Hash256 authorityPublicKey;

    @Override
    public String toString() {
        return "Precommit{" +
                "targetHash=" + targetHash +
                ", targetNumber=" + targetNumber +
                ", signature=" + signature +
                ", authorityPublicKey=" + authorityPublicKey +
                '}';
    }
}

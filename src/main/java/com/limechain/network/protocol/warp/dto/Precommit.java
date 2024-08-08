package com.limechain.network.protocol.warp.dto;

import com.limechain.polkaj.Hash256;
import com.limechain.polkaj.Hash512;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigInteger;

@Data
@AllArgsConstructor
@NoArgsConstructor
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

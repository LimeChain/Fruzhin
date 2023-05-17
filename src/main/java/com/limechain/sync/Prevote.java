package com.limechain.sync;

import io.emeraldpay.polkaj.types.Hash256;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigInteger;

@Getter
@Setter
@AllArgsConstructor
public class Prevote {
    Hash256 targetHash;
    BigInteger targetNumber;
}

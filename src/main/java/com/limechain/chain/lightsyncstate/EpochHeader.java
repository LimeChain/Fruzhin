package com.limechain.chain.lightsyncstate;

import lombok.Getter;
import lombok.Setter;

import java.math.BigInteger;

@Getter
@Setter
public class EpochHeader {
    private BigInteger startSlot;
    private BigInteger endSlot;
}

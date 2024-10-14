package com.limechain.runtime.babeapi;

import com.limechain.chain.lightsyncstate.Authority;
import com.limechain.chain.lightsyncstate.BabeEpoch;
import lombok.Getter;
import lombok.Setter;
import org.javatuples.Pair;

import java.math.BigInteger;
import java.util.List;


/**
 * Current configuration of the BABE consensus protocol.
 */
@Getter
@Setter
public class BabeApiConfiguration {
    private BigInteger slotDuration;
    private BigInteger epochLength;
    private Pair<BigInteger, BigInteger> constant;
    private List<Authority> authorities;
    private byte[] randomness;
    private BabeEpoch.BabeAllowedSlots allowedSlots;
}

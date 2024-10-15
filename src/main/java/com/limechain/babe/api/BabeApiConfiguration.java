package com.limechain.babe.api;

import com.limechain.chain.lightsyncstate.Authority;
import com.limechain.chain.lightsyncstate.BabeEpoch;
import lombok.Data;
import org.javatuples.Pair;

import java.math.BigInteger;
import java.util.List;


/**
 * Current configuration of the BABE consensus protocol.
 */
@Data
public class BabeApiConfiguration {
    private BigInteger slotDuration;
    private BigInteger epochLength;
    private Pair<BigInteger, BigInteger> constant;
    private List<Authority> authorities;
    private byte[] randomness;
    private BabeEpoch.BabeAllowedSlots allowedSlots;
}

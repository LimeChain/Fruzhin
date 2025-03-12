package com.limechain.consensus.babe.dto.message;

import com.limechain.chain.lightsyncstate.BabeEpoch;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.javatuples.Pair;

import java.math.BigInteger;

/**
 * Represents the BABE constant and secondary slot.
 */
@Getter
@AllArgsConstructor
public class EpochDescriptor {
    private Pair<BigInteger, BigInteger> constant;
    private BabeEpoch.BabeAllowedSlots allowedSlots;
}

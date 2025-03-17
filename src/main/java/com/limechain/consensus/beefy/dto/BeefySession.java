package com.limechain.consensus.beefy.dto;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.javatuples.Pair;

import java.math.BigInteger;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Value
@RequiredArgsConstructor
public class BeefySession {

    BeefyAuthoritySet authoritySet;
    // Key is non-mandatory block
    Map<Commitment, BeefyRound> rounds = new ConcurrentHashMap();

    BigInteger mandatoryBlock;

    boolean isMandatoryBlockFinalized;

    @Nullable
    BigInteger highestFinalizedForSession;

    @Nullable
    Pair<byte[], byte[]> beefyKeyPair;
}


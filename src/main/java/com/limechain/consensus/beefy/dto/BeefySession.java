package com.limechain.consensus.beefy.dto;

import jakarta.annotation.Nullable;
import lombok.Data;
import lombok.Setter;
import org.javatuples.Pair;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

@Data
public class BeefySession {

    private final BeefyAuthoritySet authoritySet;

    private Map<Commitment, BeefyRound> rounds = new HashMap<>();

    private final BigInteger mandatoryBlock;

    @Setter
    private boolean isMandatoryBlockFinalized;

    @Setter
    @Nullable
    private BigInteger highestFinalizedForSession;

    @Nullable
    private final Pair<byte[], byte[]> beefyKeyPair;
}


package com.limechain.consensus.beefy.dto;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import org.javatuples.Pair;

import java.math.BigInteger;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Value
@RequiredArgsConstructor
public class BeefySession {

    BeefyAuthoritySet authoritySet;
    Set<BigInteger> nonMandatoryBlockNumbers = Collections.synchronizedSet(new HashSet<>());
    @Nullable
    Pair<byte[], byte[]> beefyKeyPair;
}


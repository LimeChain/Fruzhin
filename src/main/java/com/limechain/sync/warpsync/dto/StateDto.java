package com.limechain.sync.warpsync.dto;

import org.javatuples.Pair;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.List;

public record StateDto(BigInteger latestRound, String lastFinalizedBlockHash, BigInteger lastFinalizedBlockNumber,
                       List<Pair<String, BigInteger>> authoritySet, BigInteger setId)
        implements Serializable {
}

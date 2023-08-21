package com.limechain.sync.warpsync.dto;

import java.io.Serializable;
import java.math.BigInteger;

public record StateDto(BigInteger latestRound, String lastFinalizedBlockHash, BigInteger lastFinalizedBlockNumber)
        implements Serializable {
}

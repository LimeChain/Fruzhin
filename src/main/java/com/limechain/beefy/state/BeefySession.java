package com.limechain.beefy.state;

import com.limechain.beefy.dto.ValidatorSet;
import lombok.Value;

import java.math.BigInteger;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Value
public class BeefySession {
    ValidatorSet validatorSet;
    // Key is non-mandatory block
    Map<BigInteger, BeefyRound> rounds = new ConcurrentHashMap();
}

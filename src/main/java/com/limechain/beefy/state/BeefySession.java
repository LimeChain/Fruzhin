package com.limechain.beefy.state;

import com.limechain.beefy.dto.ValidatorSet;
import lombok.Value;

import java.math.BigInteger;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Value
public class BeefySession {
    ValidatorSet validatorSet;
    Set<BigInteger> nonMandatoryBlockNumbers = Collections.synchronizedSet(new HashSet<>());
}


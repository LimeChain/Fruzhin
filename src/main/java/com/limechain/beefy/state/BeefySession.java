package com.limechain.beefy.state;

import com.limechain.beefy.dto.ValidatorSet;
import lombok.Data;

import java.math.BigInteger;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

@Data
public class BeefySession {
    private ValidatorSet validatorSet;
    private Set<BigInteger> nonMandatoryBlockNumbers = Collections.synchronizedSortedSet(new TreeSet<>());
}

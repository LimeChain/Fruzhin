package com.limechain.beefy.state;

import com.limechain.beefy.dto.ValidatorSet;
import lombok.Data;
import org.javatuples.Pair;

import java.math.BigInteger;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Data
public class BeefySession {
    ValidatorSet validatorSet;
    Set<BigInteger> nonMandatoryBlockNumbers = Collections.synchronizedSet(new HashSet<>());
    Pair<byte[], byte[]> beefyKeyPair = null;

    public BeefySession(ValidatorSet validatorSet) {
        this.validatorSet = validatorSet;
    }
}


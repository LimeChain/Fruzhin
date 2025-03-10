package com.limechain.beefy.state;

import com.limechain.grandpa.state.AuthoritySet;
import lombok.Data;
import org.javatuples.Pair;

import java.math.BigInteger;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Data
public class BeefySession {
    AuthoritySet authoritySet;
    Set<BigInteger> nonMandatoryBlockNumbers = Collections.synchronizedSet(new HashSet<>());
    Pair<byte[], byte[]> beefyKeyPair = null;

    public BeefySession(AuthoritySet authoritySet) {
        this.authoritySet = authoritySet;
    }
}


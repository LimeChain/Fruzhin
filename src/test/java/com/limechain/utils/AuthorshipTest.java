package com.limechain.utils;

import com.limechain.babe.api.Authorship;
import com.limechain.chain.lightsyncstate.Authority;
import org.javatuples.Pair;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuthorshipTest {

    @Test
    void testCalculatePrimaryThreshold() {
        var constant = new Pair<>(BigInteger.ONE, BigInteger.valueOf(4));
        Authority authority1 = new Authority(new byte[32], BigInteger.ONE);
        Authority authority2 = new Authority(new byte[32], BigInteger.ONE);
        Authority authority3 = new Authority(new byte[32], BigInteger.ONE);

        var authorities = List.of(authority1, authority2, authority3);
        var result = Authorship.calculatePrimaryThreshold(constant, authorities, 0);
        assertEquals(new BigInteger("31115318766088776340791719032032067584"), result);
    }
}

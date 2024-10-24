package com.limechain.babe;

import com.limechain.chain.lightsyncstate.Authority;
import com.limechain.utils.StringUtils;
import io.emeraldpay.polkaj.schnorrkel.Schnorrkel;
import org.javatuples.Pair;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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


    @Test
    void testCalculatePrimaryThresholdWithConstantNumeratorEqualsZero() {
        var constant = new Pair<>(BigInteger.ZERO, BigInteger.valueOf(4));
        Authority authority1 = new Authority(new byte[32], BigInteger.ONE);
        Authority authority2 = new Authority(new byte[32], BigInteger.ONE);
        Authority authority3 = new Authority(new byte[32], BigInteger.ONE);

        var authorities = List.of(authority1, authority2, authority3);
        var result = Authorship.calculatePrimaryThreshold(constant, authorities, 0);
        assertEquals(new BigInteger("0"), result);
    }

    @Test
    void testCalculatePrimaryThresholdWithConstantDenominatorEqualsZero() {
        var constant = new Pair<>(BigInteger.ONE, BigInteger.ZERO);
        Authority authority1 = new Authority(new byte[32], BigInteger.ONE);
        Authority authority2 = new Authority(new byte[32], BigInteger.ONE);
        Authority authority3 = new Authority(new byte[32], BigInteger.ONE);

        var authorities = List.of(authority1, authority2, authority3);
        assertThrows(IllegalArgumentException.class,
                () -> Authorship.calculatePrimaryThreshold(constant, authorities, 0));
    }

    @Test
    void testCalculatePrimaryThresholdWithBabeConstantOutOfRange() {
        var constant = new Pair<>(BigInteger.TEN, BigInteger.ONE);
        Authority authority1 = new Authority(new byte[32], BigInteger.ONE);
        Authority authority2 = new Authority(new byte[32], BigInteger.ONE);
        Authority authority3 = new Authority(new byte[32], BigInteger.ONE);

        var authorities = List.of(authority1, authority2, authority3);
        assertThrows(IllegalStateException.class,
                () -> Authorship.calculatePrimaryThreshold(constant, authorities, 0));
    }

    @Test
    void testCalculatePrimaryThresholdWithAuthorityIndexOutOfBounds() {
        var constant = new Pair<>(BigInteger.ONE, BigInteger.valueOf(4));
        Authority authority1 = new Authority(new byte[32], BigInteger.ONE);
        Authority authority2 = new Authority(new byte[32], BigInteger.ONE);
        Authority authority3 = new Authority(new byte[32], BigInteger.ONE);

        var authorities = List.of(authority1, authority2, authority3);
        var nonExistingIndex = Integer.MAX_VALUE;
        assertThrows(IllegalArgumentException.class,
                () -> Authorship.calculatePrimaryThreshold(constant, authorities, nonExistingIndex));
    }

    @Test
    void testCalculatePrimaryThresholdWithNegativeAuthorityIndex() {
        var constant = new Pair<>(BigInteger.ONE, BigInteger.valueOf(4));
        Authority authority1 = new Authority(new byte[32], BigInteger.ONE);
        Authority authority2 = new Authority(new byte[32], BigInteger.ONE);
        Authority authority3 = new Authority(new byte[32], BigInteger.ONE);

        var authorities = List.of(authority1, authority2, authority3);
        assertThrows(IllegalArgumentException.class,
                () -> Authorship.calculatePrimaryThreshold(constant, authorities, -1));
    }

    @Test
    void testClaimPrimarySlot() {
        var pk = StringUtils.hexToBytes("0xd43593c715fdd31c61141abd04a99fd6822c8558854ccde39a5684e7a56da27d");
        var sk = StringUtils.hexToBytes("0x33a6f3093f158a7109f679410bef1a0c54168145e0cecb4df006c1c2fffb1f09");
        var full_sk = new byte[64];
        System.arraycopy(sk, 0, full_sk, 0, sk.length);
        System.arraycopy(pk, 0, full_sk, sk.length, pk.length);

        var kp = new Schnorrkel.KeyPair(
                sk,
                full_sk);

//        Authorship.claimPrimarySlot(
//                new byte[32],
//                288267318L,
//                0L,
//                0,
//                BigInteger.valueOf(2511777012606999L),
//                //TODO: Take the keypair from gossamer and use it here
//                //keypair from the logs [&{0x1400023e9b8 0x1400023e9c0}]
//                kp
//        );
    }
}

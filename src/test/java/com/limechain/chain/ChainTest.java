package com.limechain.chain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class ChainTest {

    @Test
    public void fromStringTest() {
        Chain polkadotChain = Chain.fromString("polkadot");
        assertEquals(Chain.POLKADOT, polkadotChain);

        Chain kusamaChain = Chain.fromString("kusama");
        assertEquals(Chain.KUSAMA, kusamaChain);

        Chain westendChain = Chain.fromString("westend");
        assertEquals(Chain.WESTEND, westendChain);
    }

    @Test
    public void invalidFromStringTest(){
        String testString = "string";
        Chain chain = Chain.fromString(testString);
        assertNull(chain);
    }
}

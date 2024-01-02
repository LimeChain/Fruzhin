package com.limechain.chain;

import com.limechain.rpc.config.SubscriptionName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ChainTest {

    @Test
    void fromStringTest() {
        Chain polkadotChain = Chain.fromString("polkadot");
        assertEquals(Chain.POLKADOT, polkadotChain);

        Chain kusamaChain = Chain.fromString("kusama");
        assertEquals(Chain.KUSAMA, kusamaChain);

        Chain westendChain = Chain.fromString("westend");
        assertEquals(Chain.WESTEND, westendChain);
    }

    @Test
    void FromString_returns_correctValue() {
        assertEquals(Chain.POLKADOT,
                Chain.fromString("polkadot"));
        assertNull(SubscriptionName.fromString("invalid"));
    }
}

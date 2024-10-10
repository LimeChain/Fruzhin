package com.limechain.network.kad;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KademliaServiceTest {

    @Test
    void dnsNodeToIp4_UnchangedAddress_ifInvalidDomain() {
        String bootNode = "/dns/invalid.domain.limechain/tcp/12345";

        assertEquals(bootNode, DnsUtils.dnsNodeToIp4(bootNode));
    }

    @Test
    void dnsNodeToIp4_UnchangedAddress_IfNotDns() {
        String bootNode = "/ip4/192.168.1.1/tcp/12345";

        assertEquals(bootNode, DnsUtils.dnsNodeToIp4(bootNode));
    }
}

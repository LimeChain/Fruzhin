package com.limechain.network.kad;

import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.assertEquals;

class KademliaServiceTest {

    /**
     * Test might throw UnknownHostException and fail if domain is no longer accessible
     *
     * @throws UnknownHostException
     */
    @Test
    void dnsNodeToIp4_TransformDnsNode() throws UnknownHostException {
        String bootNode = "/dns/p2p.0.polkadot.network/tcp/30333/p2p/12D3KooWHsvEicXjWWraktbZ4MQBizuyADQtuEGr3NbDvtm5rFA5";
        InetAddress address = InetAddress.getByName("p2p.0.polkadot.network");
        String expected = "/ip4/" + address.getHostAddress() + "/tcp/30333/p2p/12D3KooWHsvEicXjWWraktbZ4MQBizuyADQtuEGr3NbDvtm5rFA5";

        assertEquals(expected, DnsUtils.dnsNodeToIp4(bootNode));
    }

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

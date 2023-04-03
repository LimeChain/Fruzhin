package com.limechain.network.kad;

import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Level;

import static com.limechain.network.kad.KademliaService.dnsNodeToIp4;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class KademliaServiceTest {

    /**
     * Test might throw UnknownHostException and fail if domain is no longer accessible
     *
     * @throws UnknownHostException
     */
    @Test
    public void dnsNodeToIp4_TransformDnsNode() throws UnknownHostException {
        String bootNode = "/dns/p2p.0.polkadot.network/tcp/30333/p2p/12D3KooWHsvEicXjWWraktbZ4MQBizuyADQtuEGr3NbDvtm5rFA5";
        InetAddress address = InetAddress.getByName("p2p.0.polkadot.network");
        String expected = "/ip4/" + address.getHostAddress() + "/tcp/30333/p2p/12D3KooWHsvEicXjWWraktbZ4MQBizuyADQtuEGr3NbDvtm5rFA5";
        assertEquals(expected, dnsNodeToIp4(bootNode));
    }

    @Test
    public void dnsNodeToIp4_UnchangedAddress_ifInvalidDomain() {
        String bootNode2 = "/dns/invalid.domain.limechain/tcp/12345";
        assertEquals(bootNode2, dnsNodeToIp4(bootNode2));
    }

    @Test
    public void dnsNodeToIp4_UnchangedAddress_IfNotDns() {
        String bootNode3 = "/ip4/192.168.1.1/tcp/12345";
        assertEquals(bootNode3, dnsNodeToIp4(bootNode3));
    }
}

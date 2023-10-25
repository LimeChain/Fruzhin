package com.limechain.config;

import com.limechain.chain.Chain;
import com.limechain.network.Network;
import com.limechain.network.protocol.blockannounce.NodeRole;
import io.libp2p.core.Host;
import io.libp2p.core.PeerId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SystemInfoTest {

    @Test
    void SystemInfo_SetsRole_NoCliOption() {
        HostConfig hostConfig = mock(HostConfig.class);
        when(hostConfig.getChain()).thenReturn(Chain.POLKADOT);
        when(hostConfig.getRocksDbPath()).thenReturn("./test/db");

        Network network = mock(Network.class);
        Host host = mock(Host.class);
        when(host.getPeerId()).thenReturn(PeerId.fromBase58("12D3KooWRHfNJwkKeSJWD28hYFyA18dcN9qU1JEzJJaguarDPS"));
        when(network.getHost()).thenReturn(host);
        when(network.getNodeRole()).thenReturn(NodeRole.FULL);

        SystemInfo systemInfo = new SystemInfo(hostConfig, network);
        String expectedRole = NodeRole.FULL.name();
        Chain expectedChain = Chain.POLKADOT;
        String expectedDbPath = "./test/db";
        String expectedHostIdentity = "12D3KooWRHfNJwkKeSJWD28hYFyA18dcN9qU1JEzJJaguarDPS";

        assertEquals(expectedRole, systemInfo.getRole());
        assertEquals(expectedChain, systemInfo.getChain());
        assertEquals(expectedDbPath, systemInfo.getDbPath());
        assertEquals(expectedHostIdentity, systemInfo.getHostIdentity());
    }
}

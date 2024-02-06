package com.limechain.rpc.methods.system;

import com.limechain.chain.ChainService;
import com.limechain.chain.spec.ChainSpec;
import com.limechain.chain.spec.ChainType;
import com.limechain.config.SystemInfo;
import com.limechain.network.Network;
import com.limechain.sync.warpsync.WarpSyncMachine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SystemRPCImplTest {
    // Class to be tested
    private SystemRPCImpl systemRPC;

    // Dependencies
    private ChainService chainService;
    private WarpSyncMachine warpSync;
    private Network network;
    private SystemInfo systemInfo;

    @BeforeEach
    public void setup() {
        chainService = mock(ChainService.class);
        systemInfo = mock(SystemInfo.class);
        warpSync = mock(WarpSyncMachine.class);
        network = mock(Network.class);
        systemRPC = new SystemRPCImpl(chainService, systemInfo, network, warpSync);
    }

    @Test
    void systemName() {
        when(systemInfo.getHostName()).thenReturn("Java Host");

        assertEquals("Java Host", systemRPC.systemName());
    }

    @Test
    void systemVersion() {
        when(systemInfo.getHostVersion()).thenReturn("0.1");

        assertEquals("0.1", systemRPC.systemVersion());
    }

    @Test
    void systemNodeRoles() {
        when(systemInfo.getRole()).thenReturn("Light Client");

        assertArrayEquals(new String[]{"Light Client"}, systemRPC.systemNodeRoles());
    }

    @Test
    void systemChain() {
        ChainSpec chainSpec = mock(ChainSpec.class);
        when(chainSpec.getName()).thenReturn("Polkadot");

        when(chainService.getChainSpec()).thenReturn(chainSpec);

        assertEquals("Polkadot", systemRPC.systemChain());
    }

    @Test
    void systemChainType() {
        ChainSpec chainSpec = mock(ChainSpec.class);
        when(chainSpec.getChainType()).thenReturn(ChainType.LIVE);
        when(chainService.getChainSpec()).thenReturn(chainSpec);

        assertEquals(ChainType.LIVE, systemRPC.systemChainType());
    }

}

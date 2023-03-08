package com.limechain.rpc.methods.system;

import com.limechain.chain.ChainService;
import com.limechain.chain.ChainSpec;
import com.limechain.config.SystemInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SystemRPCImplTest {
    // Class to be tested
    private SystemRPCImpl systemRPC;

    // Dependencies
    private ChainService chainService;
    private SystemInfo systemInfo;

    @BeforeEach
    public void setup () {
        chainService = mock(ChainService.class);
        systemInfo = mock(SystemInfo.class);

        systemRPC = new SystemRPCImpl(chainService, systemInfo);
    }

    @Test
    public void systemName () {
        when(systemInfo.getHostName()).thenReturn("Java Host");

        assertEquals(systemRPC.systemName(), "Java Host");
    }

    @Test
    public void systemVersion () {
        when(systemInfo.getHostVersion()).thenReturn("0.1");

        assertEquals(systemRPC.systemVersion(), "0.1");
    }


    @Test
    public void systemNodeRoles () {
        when(systemInfo.getRole()).thenReturn("Light Client");

        assertArrayEquals(systemRPC.systemNodeRoles(), new String[]{"Light Client"});
    }

    @Test
    public void systemChain () {
        ChainSpec chainSpec = new ChainSpec();
        chainSpec.setName("Polkadot");
        when(chainService.getGenesis()).thenReturn(chainSpec);

        assertEquals(systemRPC.systemChain(), "Polkadot");
    }

    @Test
    public void systemChainType () {
        ChainSpec chainSpec = new ChainSpec();
        chainSpec.setChainType("Polkadot - Live");
        when(chainService.getGenesis()).thenReturn(chainSpec);

        assertEquals(systemRPC.systemChainType(), "Polkadot - Live");
    }

}

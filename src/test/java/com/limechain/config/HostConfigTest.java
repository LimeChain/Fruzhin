package com.limechain.config;

import com.limechain.chain.Chain;
import com.limechain.cli.CliArguments;
import com.limechain.storage.RocksDBInitializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

public class HostConfigTest {
    private CliArguments cliArguments;

    @BeforeEach
    public void setup(){
        cliArguments = mock(CliArguments.class);
    }

    @Test
    public void HostConfig_Succeeds_PassedCliArguments() {
        when(cliArguments.network()).thenReturn(Chain.WESTEND.getValue());
        when(cliArguments.dbPath()).thenReturn(RocksDBInitializer.testDirectory);

        HostConfig hostConfig = new HostConfig(cliArguments);
        assertEquals(Chain.WESTEND, hostConfig.getChain());

        String westendGenesisPath = "genesis/westend.json";
        setField(hostConfig, "westendGenesisPath", westendGenesisPath);

        assertEquals(westendGenesisPath, hostConfig.getGenesisPath());
    }
}

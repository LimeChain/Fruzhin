package com.limechain.config;

import com.limechain.chain.Chain;
import com.limechain.cli.CliArguments;
import com.limechain.exception.misc.InvalidChainException;
import com.limechain.network.protocol.blockannounce.NodeRole;
import com.limechain.storage.DBInitializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

class HostConfigTest {
    private final String westendGenesisPath = "genesis/westend2.json";
    private final String kusamaGenesisPath = "genesis/ksmcc3.json";
    private final String polkadotGenesisPath = "genesis/polkadot.json";
    private CliArguments cliArguments;

    @BeforeEach
    public void setup() {
        cliArguments = mock(CliArguments.class);
    }

    @Test
    void HostConfig_Succeeds_PassedCliArguments() {
        when(cliArguments.network()).thenReturn(Chain.WESTEND.getValue());
        when(cliArguments.nodeRole()).thenReturn("full");
        when(cliArguments.dbPath()).thenReturn(DBInitializer.DEFAULT_DIRECTORY);

        HostConfig hostConfig = new HostConfig(cliArguments);
        assertEquals(Chain.WESTEND, hostConfig.getChain());

        setField(hostConfig, "westendGenesisPath", westendGenesisPath);

        assertEquals(westendGenesisPath, hostConfig.getGenesisPath());
    }

    @Test
    void HostConfig_throwsException_whenNetworkInvalid() {
        when(cliArguments.network()).thenReturn("invalidNetwork");
        when(cliArguments.nodeRole()).thenReturn("full");
        when(cliArguments.dbPath()).thenReturn(DBInitializer.DEFAULT_DIRECTORY);
        assertThrows(InvalidChainException.class, () -> new HostConfig(cliArguments));
    }

    @Test
    void GetGenesisPath_returnsCorrectPath_whenPassedChain() {
        // Westend
        when(cliArguments.network()).thenReturn(Chain.WESTEND.getValue());
        when(cliArguments.nodeRole()).thenReturn(NodeRole.FULL.toString().toLowerCase());
        when(cliArguments.dbPath()).thenReturn(DBInitializer.DEFAULT_DIRECTORY);
        HostConfig hostConfig = new HostConfig(cliArguments);
        setField(hostConfig, "westendGenesisPath", westendGenesisPath);
        assertEquals(hostConfig.getGenesisPath(), westendGenesisPath);

        // Kusama
        when(cliArguments.network()).thenReturn(Chain.KUSAMA.getValue());
        hostConfig = new HostConfig(cliArguments);
        setField(hostConfig, "kusamaGenesisPath", kusamaGenesisPath);
        assertEquals(hostConfig.getGenesisPath(), kusamaGenesisPath);

        // Polkadot
        when(cliArguments.network()).thenReturn(Chain.POLKADOT.getValue());
        hostConfig = new HostConfig(cliArguments);
        setField(hostConfig, "polkadotGenesisPath", polkadotGenesisPath);
        assertEquals(hostConfig.getGenesisPath(), polkadotGenesisPath);
    }
}

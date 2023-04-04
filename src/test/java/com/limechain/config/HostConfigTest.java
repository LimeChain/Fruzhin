package com.limechain.config;

import com.limechain.chain.Chain;
import com.limechain.cli.CliArguments;
import com.limechain.storage.DBInitializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.util.ReflectionTestUtils.setField;

public class HostConfigTest {
    private final String westendGenesisPath = "genesis/westend.json";
    private final String kusamaGenesisPath = "genesis/kusama.json";
    private final String polkadotGenesisPath = "genesis/polkadot.json";
    private CliArguments cliArguments;

    @BeforeEach
    public void setup() {
        cliArguments = mock(CliArguments.class);
    }

    @Test
    public void HostConfig_Succeeds_PassedCliArguments() {
        when(cliArguments.network()).thenReturn(Chain.WESTEND.getValue());
        when(cliArguments.dbPath()).thenReturn(DBInitializer.DEFAULT_DIRECTORY);

        HostConfig hostConfig = new HostConfig(cliArguments);
        assertEquals(Chain.WESTEND, hostConfig.getChain());

        setField(hostConfig, "westendGenesisPath", westendGenesisPath);

        assertEquals(westendGenesisPath, hostConfig.getGenesisPath());
    }

    @Test
    public void HostConfig_throwsException_whenNetworkInvalid() {
        when(cliArguments.network()).thenReturn("invalidNetwork");
        when(cliArguments.dbPath()).thenReturn(DBInitializer.DEFAULT_DIRECTORY);
        Exception exception = assertThrows(RuntimeException.class, () -> {
            new HostConfig(cliArguments);
        });

        String expectedMessage = "Unsupported or unknown network";
        String actualMessage = exception.getMessage();

        assertTrue(actualMessage.contains(expectedMessage));
    }

    @Test
    public void GetGenesisPath_returnsCorrectPath_whenPassedChain() {
        // Westend
        when(cliArguments.network()).thenReturn(Chain.WESTEND.getValue());
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

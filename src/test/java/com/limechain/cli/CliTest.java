package com.limechain.cli;

import com.limechain.storage.RocksDBInitializer;
import org.apache.commons.cli.Options;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CliTest {
    private Cli cli;

    @BeforeEach
    public void setup() {
        cli = new Cli();
    }

    @Test
    public void buildOptions_buildsOptions() {
        Options options = cli.getOptions();
        assertNotNull(options);
        assertTrue(options.hasOption("network"));
        assertTrue(options.hasOption("n"));
        assertTrue(options.hasOption("db-path"));
        assertEquals(options.getRequiredOptions().size(), 0);
    }

    @Test
    public void parseArgs_returns_networkParameter() {
        CliArguments arguments = cli.parseArgs(new String[]{"--network", "polkadot"});
        assertEquals(arguments.network(), "polkadot");
    }

    @Test
    public void parseArgs_returns_defaultValue() {
        CliArguments arguments = cli.parseArgs(new String[]{});
        assertEquals(arguments.network(), "");
    }

    @Test
    public void parseArgs_returns_shortNetworkParameter() {
        CliArguments arguments = cli.parseArgs(new String[]{"--n", "polkadot"});
        assertEquals(arguments.network(), "polkadot");
    }

    @Test
    public void parseArgs_returns_dbPathParameter() {
        CliArguments arguments = cli.parseArgs(new String[]{"--db-path", "./test-path-somewhere"});
        assertEquals(arguments.dbPath(), "./test-path-somewhere");
    }

    @Test
    public void parseArgs_returns_defaultDbPathParameter() {
        CliArguments arguments = cli.parseArgs(new String[]{});
        assertEquals(arguments.dbPath(), RocksDBInitializer.defaultDirectory);
    }

    @Test
    public void parseArgs_throws_whenInvalidArguments() {

        assertThrows(RuntimeException.class, () -> cli.parseArgs(new String[]{"--network"}));

        assertThrows(RuntimeException.class, () -> cli.parseArgs(new String[]{"-network"}));

        assertThrows(RuntimeException.class, () -> cli.parseArgs(new String[]{"--unsupportedParam"}));

    }

}

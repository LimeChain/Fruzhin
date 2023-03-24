package com.limechain.cli;

import com.limechain.storage.DBInitializer;
import lombok.Getter;
import lombok.extern.java.Log;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.util.logging.Level;

/**
 * Abstraction class around apache.commons.cli used to set arguments rules and parse node arguments
 */
@Getter
@Log
public class Cli {
    /**
     * Holds CLI options
     */
    private final Options options;

    private final HelpFormatter formatter = new HelpFormatter();

    public Cli() {
        this.options = buildOptions();
    }

    /**
     * Parses node launch arguments.
     *
     * @param args launch arguments coming from the console
     * @return {@link CliArguments} that contain the successfully parsed arguments
     * @throws RuntimeException if arguments don't follow the argument rules set by {@link #buildOptions()}
     */
    public CliArguments parseArgs(String[] args) {
        try {
            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args);
            String network = cmd.getOptionValue("network", "").toLowerCase();
            String dbPath = cmd.getOptionValue("db-path", DBInitializer.DEFAULT_DIRECTORY);

            return new CliArguments(network, dbPath);
        } catch (ParseException e) {
            log.log(Level.SEVERE, "Failed to parse cli arguments", e);
            formatter.printHelp("Specify the network name - polkadot, kusama, westend, local", options);
            throw new RuntimeException();
        }
    }

    /**
     * Configures and builds argument rules accepted when running the node.
     *
     * @return configured options
     */
    private Options buildOptions() {
        Options options = new Options();
        Option networkOption = new Option("n", "network", true, "Client network");
        Option dbPathOption = new Option(null, "db-path", true, "RocksDB path");
        networkOption.setRequired(false);
        dbPathOption.setRequired(false);

        options.addOption(networkOption);
        options.addOption(dbPathOption);
        return options;
    }

}

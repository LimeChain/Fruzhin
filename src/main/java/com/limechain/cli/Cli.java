package com.limechain.cli;


import com.limechain.storage.RocksDBInitializer;
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

@Getter
@Log
public class Cli {

    private final Options options;
    private final HelpFormatter formatter = new HelpFormatter();


    public Cli () {
        Options options = new Options();
        Option networkOption = new Option("n", "network", true, "Client network");
        Option dbPathOption = new Option(null, "db-path", true, "RocksDB path");
        networkOption.setRequired(false);
        dbPathOption.setRequired(false);

        options.addOption(networkOption);
        options.addOption(dbPathOption);

        this.options = options;

    }

    public CliArguments parseArgs (String[] args) {
        try {
            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args);
            String network = cmd.getOptionValue("network", "").toLowerCase();
            String dbPath = cmd.getOptionValue("db-path", RocksDBInitializer.defaultDirectory);

            return new CliArguments(network, dbPath);
        } catch (ParseException e) {
            log.log(Level.SEVERE, "Failed to parse cli arguments", e);
            formatter.printHelp("Specify the network name - polkadot, kusama, westend", options);
            throw new RuntimeException();
        }
    }

}

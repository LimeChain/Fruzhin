package com.limechain.config;

import com.limechain.chain.Chain;
import com.limechain.storage.RocksDBInitializer;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.limechain.chain.Chain.KUSAMA;
import static com.limechain.chain.Chain.POLKADOT;
import static com.limechain.chain.Chain.WESTEND;

public class HostConfig {
    private static final Logger LOGGER = Logger.getLogger(HostConfig.class.toString());
    public String genesisPath;
    public Chain chain;
    public String rocksDbPath;

    @Value("${genesis.path.polkadot}")
    private String polkadotGenesisPath;
    @Value("${genesis.path.kusama}")
    private String kusamaGenesisPath;
    @Value("${genesis.path.westend}")
    private String westendGenesisPath;

    @Value("${helper.node.address}")
    public String helperNodeAddress;

    public HostConfig(String[] args) {
        // Setup CLI arguments
        Options options = new Options();
        Option networkOption = new Option("n", "network", true, "client network");
        Option dbPathOption = new Option(null, "db-path", true, "RocksDB path");
        networkOption.setRequired(false);
        dbPathOption.setRequired(false);

        options.addOption(networkOption);
        options.addOption(dbPathOption);

        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd; // Not a good practice probably

        // Try to parse the arguments
        try {
            CommandLineParser parser = new DefaultParser();
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            LOGGER.log(Level.SEVERE, "Failed to parse cli arguments", e);
            formatter.printHelp("Specify the network name - polkadot, kusama, westend", options);
            throw new RuntimeException();
        }

        // Get the network argument
        String network = cmd.getOptionValue("network", "").toLowerCase();

        // Read configuration file
        // Map network argument to chain spec patch
        try {
            boolean isNetworkStored = storeMatchedNetwork(network);
            if (!isNetworkStored) {
                throw new IOException("Unsupported or unknown network");
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to load genesis path", e);
            throw new RuntimeException();
        }

        this.rocksDbPath = cmd.getOptionValue("db-path", RocksDBInitializer.defaultDirectory);

        LOGGER.log(Level.INFO, String.format("✅️Loaded app config for chain %s%n", chain));
    }

    private boolean storeMatchedNetwork(String network) {
        if (network == POLKADOT.getValue()) {
            this.genesisPath = polkadotGenesisPath;
            this.chain = POLKADOT;
            return true;
        }
        if (network == KUSAMA.getValue()) {
            this.genesisPath = kusamaGenesisPath;
            this.chain = Chain.KUSAMA;
            return true;
        }
        // Empty string case because we want the default network to be Westend
        if (network == WESTEND.getValue() || network.isEmpty()) {
            this.genesisPath = westendGenesisPath;
            this.chain = Chain.WESTEND;
            return true;
        }
        return false;
    }
}

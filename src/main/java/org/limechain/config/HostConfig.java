package org.limechain.config;

import org.apache.commons.cli.*;
import org.limechain.chain.Chain;
import org.limechain.storage.RocksDBInitializer;

import java.io.IOException;
import java.util.Properties;

public class HostConfig extends Config {
    public String genesisPath;
    public Chain chain;
    public String rocksDbPath;
    public String helperNodeAddress;

    public HostConfig (String[] args) {
        // Setup CLI arguments
        Options options = new Options();
        Option input = new Option("n", "network", true, "client network");
        Option rocksDbPathOption = new Option(null, "db-path", true, "RocksDB path");
        input.setRequired(false);
        rocksDbPathOption.setRequired(false);

        options.addOption(input);
        options.addOption(rocksDbPathOption);

        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null; // Not a good practice probably

        // Try to parse the arguments
        try {
            CommandLineParser parser = new DefaultParser();
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("Specify the network name - polkadot, kusama, westend", options);
            System.exit(1);
        }

        // Get the network argument
        String network = cmd.getOptionValue("network", "");

        Properties properties = this.readConfig();

        this.helperNodeAddress = properties.get("HELPER_NODE_ADDRESS").toString();
        // Read configuration file
        // Map network argument to chain spec patch
        try {
            switch (network.toLowerCase()) {
                case "polkadot" -> {
                    this.genesisPath = properties.get("POLKADOT_GENESIS_PATH").toString();
                    this.chain = Chain.POLKADOT;
                }
                case "kusama" -> {
                    this.genesisPath = properties.get("KUSAMA_GENESIS_PATH").toString();
                    this.chain = Chain.KUSAMA;
                }
                // Empty string case because we want the default network to be Westend
                case "", "westend" -> {
                    this.genesisPath = properties.get("WESTEND_GENESIS_PATH").toString();
                    this.chain = Chain.WESTEND;
                }
                default -> throw new IOException("Unsupported or unknown network");
            }
        } catch (IOException ioException) {
            System.out.println("Failed to load genesis path");
            System.exit(1);
        }


        this.rocksDbPath = cmd.getOptionValue("rocksdb", RocksDBInitializer.defaultDirectory);

        System.out.printf("✅️Loaded app config for chain %s%n", chain);
    }
}

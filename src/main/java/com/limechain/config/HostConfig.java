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

import java.io.IOException;
import java.util.Properties;

import static com.limechain.chain.Chain.KUSAMA;
import static com.limechain.chain.Chain.POLKADOT;
import static com.limechain.chain.Chain.WESTEND;

public class HostConfig extends Config {
    public String genesisPath;
    public Chain chain;
    public String rocksDbPath;
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
        String network = cmd.getOptionValue("network", "").toLowerCase();

        Properties properties = this.readConfig();

        this.helperNodeAddress = properties.get("HELPER_NODE_ADDRESS").toString();
        // Read configuration file
        // Map network argument to chain spec patch
        try {
            boolean isNetworkStored = storeMatchedNetwork(network, properties);
            if (!isNetworkStored) {
                throw new IOException("Unsupported or unknown network");
            }
        } catch (IOException ioException) {
            System.out.println("Failed to load genesis path");
            System.exit(1);
        }


        this.rocksDbPath = cmd.getOptionValue("db-path", RocksDBInitializer.defaultDirectory);

        System.out.printf("✅️Loaded app config for chain %s%n", chain);
    }

    private boolean storeMatchedNetwork(String network, Properties properties) {
        if (network == POLKADOT.getValue()) {
            this.genesisPath = properties.get("POLKADOT_GENESIS_PATH").toString();
            this.chain = POLKADOT;
            return true;
        }
        if (network == KUSAMA.getValue()) {
            this.genesisPath = properties.get("KUSAMA_GENESIS_PATH").toString();
            this.chain = Chain.KUSAMA;
            return true;
        }
        // Empty string case because we want the default network to be Westend
        if (network == WESTEND.getValue() || network.isEmpty()) {
            this.genesisPath = properties.get("WESTEND_GENESIS_PATH").toString();
            this.chain = Chain.WESTEND;
            return true;
        }
        return false;
    }
}

package org.limechain.config;

import org.apache.commons.cli.*;
import org.limechain.chain.Chain;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public class AppConfig {
    private static final String CONFIG_FILE_NAME = "app.config";
    public String genesisPath;
    public Chain chain;
    public AppConfig(String[] args) {
        // Setup CLI arguments
        Options options = new Options();
        Option input = new Option("n", "network", true, "client network");
        input.setRequired(false);
        options.addOption(input);

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
        String network  = cmd.getOptionValue("network","");

        // Read configuration file
        try (FileInputStream fis = new FileInputStream(CONFIG_FILE_NAME)) {
            Properties prop = new Properties();
            prop.load(fis);

            // Map network argument to chain spec patch
            switch (network.toLowerCase()) {
                case "polkadot" -> {
                    this.genesisPath = prop.get("POLKADOT_GENESIS_PATH").toString();
                    this.chain = Chain.POLKADOT;
                }
                case "kusama" -> {
                    this.genesisPath = prop.get("KUSAMA_GENESIS_PATH").toString();
                    this.chain = Chain.KUSAMA;
                }
                // Empty string case because we want the default network to be Westend
                case "", "westend" -> {
                    this.genesisPath = prop.get("WESTEND_GENESIS_PATH").toString();
                    this.chain = Chain.WESTEND;
                }
                default -> throw new IOException("Unsupported or unknown network");
            }
            System.out.printf("✅️Loaded app config for chain %s%n", chain);
        } catch (FileNotFoundException ex) {
            System.out.printf("Failed to find the config file(%s)%n", CONFIG_FILE_NAME);
            System.exit(1);
        } catch (IOException ex) {
            System.out.printf("Failed to read the config file(%s)%n", CONFIG_FILE_NAME);
            System.exit(1);
        }
    }
}

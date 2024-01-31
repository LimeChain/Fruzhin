package com.limechain.cli;

import com.limechain.chain.Chain;
import com.limechain.exception.CliArgsParseException;
import com.limechain.network.protocol.blockannounce.NodeRole;
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

import java.util.List;

/**
 * Abstraction class around apache.commons.cli used to set arguments rules and parse node arguments
 */
@Getter
@Log
public class Cli {
    public static final String NETWORK = "network";
    public static final String DB_PATH = "db-path";
    public static final String NODE_KEY = "node-key";
    private static final String DB_RECREATE = "db-recreate";
    private static final String NODE_MODE = "node-mode";
    private static final String NO_LEGACY_PROTOCOLS = "no-legacy-protocols";
    private static final String SYNC_MODE = "sync-mode";

    /**
     * Holds CLI options
     */
    private final Options options;
    private final List<String> validChains = List.of(Chain.values()).stream().map(Chain::getValue).toList();
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
            String network = cmd.getOptionValue(NETWORK, "").toLowerCase();
            if (!validChains.contains(network) && !network.isEmpty()) throw new ParseException("Invalid network");
            String dbPath = cmd.getOptionValue(DB_PATH, DBInitializer.DEFAULT_DIRECTORY);
            boolean dbRecreate = cmd.hasOption(DB_RECREATE);
            String nodeKey = cmd.getOptionValue(NODE_KEY);
            // TODO: separation of enums; this NodeRole enum is used for blockannounce
            //       what does running the node in NodeMode NONE mean?
            String nodeMode = cmd.getOptionValue(NODE_MODE, NodeRole.FULL.toString());
            boolean noLgacyProtocols = cmd.hasOption(NO_LEGACY_PROTOCOLS);
            String syncMode = cmd.getOptionValue(SYNC_MODE, "full");

            return new CliArguments(network, dbPath, dbRecreate, nodeKey, nodeMode, noLgacyProtocols, syncMode);
        } catch (ParseException e) {
            formatter.printHelp("Specify the network name - " + String.join(", ", validChains), options);
            throw new CliArgsParseException("Failed to parse cli arguments", e);
        }
    }

    /**
     * Configures and builds argument rules accepted when running the node.
     *
     * @return configured options
     */
    private Options buildOptions() {
        Options result = new Options();
        Option networkOption = new Option("n", NETWORK, true, "Client network");
        Option dbPathOption = new Option(null, DB_PATH, true, "RocksDB path");
        Option dbClean = new Option("dbc", DB_RECREATE, false, "Clean the DB");
        Option nodeKey = new Option(null, NODE_KEY, true, "HEX for secret Ed25519 key");
        Option nodeMode = new Option("mode", NODE_MODE, true, "Node mode (light/full). " +
                "Full by default.");
        Option noLegacyProtocols = new Option(null, NO_LEGACY_PROTOCOLS, false,
                "Doesn't use legacy protocols if set");
        Option syncMode = new Option(null, SYNC_MODE, true,
                "Sync mode (warp/full)");

        networkOption.setRequired(false);
        dbPathOption.setRequired(false);
        dbClean.setRequired(false);
        nodeKey.setRequired(false);
        nodeMode.setRequired(false);
        noLegacyProtocols.setRequired(false);
        syncMode.setRequired(false);

        result.addOption(networkOption);
        result.addOption(dbPathOption);
        result.addOption(dbClean);
        result.addOption(nodeKey);
        result.addOption(nodeMode);
        result.addOption(noLegacyProtocols);
        result.addOption(syncMode);
        return result;
    }

}

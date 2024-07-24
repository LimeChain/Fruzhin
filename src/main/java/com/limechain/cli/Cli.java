package com.limechain.cli;

import com.limechain.chain.Chain;
import com.limechain.exception.misc.CliArgsParseException;
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
    private static final String CHAIN = "chain";
    private static final String NAME = "name";
    private static final String CORS = "rpc-cors";
    private static final String NO_MDNS = "no-mdns";
    private static final String NO_TELEMETRY = "no-telemetry";
    private static final String RPC_PORT = "rpc-port";
    private static final String LISTEN_ADDRESS = "listen-addr";
    private static final String BASE_PATH = "base-path";
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

            return new CliArguments(network, dbPath, dbRecreate, nodeKey);
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
        Option networkOption = new Option("n", NETWORK, true, "\nClient network");
        Option dbPathOption = new Option(null, DB_PATH, true, "\nRocksDB path");
        Option dbClean = new Option("dbc", DB_RECREATE, false, "\nClean the DB");
        Option nodeKey = new Option(null, NODE_KEY, true, "\nHEX for secret Ed25519 key");

        Option chain = new Option(null, CHAIN, true, "");
        Option name = new Option(null, NAME, true, "");
        Option cors = new Option(null, CORS, false, "");
        Option noMdns = new Option(null, NO_MDNS, false, "");
        Option noTelemetry = new Option(null, NO_TELEMETRY, false, "");
        Option rpcPort = new Option(null, RPC_PORT, true, "");
        Option listenAddress = new Option(null, LISTEN_ADDRESS, true, "");
        Option basePath = new Option(null, BASE_PATH, true, "");

        networkOption.setRequired(false);
        dbPathOption.setRequired(false);
        dbClean.setRequired(false);
        nodeKey.setRequired(false);

        chain.setRequired(false);
        name.setRequired(false);
        cors.setRequired(false);
        noMdns.setRequired(false);
        noTelemetry.setRequired(false);
        rpcPort.setRequired(false);
        listenAddress.setRequired(false);
        basePath.setRequired(false);

        result.addOption(networkOption);
        result.addOption(dbPathOption);
        result.addOption(dbClean);
        result.addOption(nodeKey);

        result.addOption(chain);
        result.addOption(name);
        result.addOption(cors);
        result.addOption(noMdns);
        result.addOption(noTelemetry);
        result.addOption(rpcPort);
        result.addOption(listenAddress);
        result.addOption(basePath);
        return result;
    }

}

package com.limechain.cli;

import com.limechain.chain.Chain;
import com.limechain.exception.CliArgsParseException;
import com.limechain.network.protocol.blockannounce.NodeRole;
import com.limechain.rpc.config.RpcMethods;
import com.limechain.storage.DBInitializer;
import com.limechain.sync.SyncMode;
import lombok.Getter;
import lombok.extern.java.Log;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.jetbrains.annotations.NotNull;

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
    public static final String PUBLIC_RPC = "public-rpc";
    public static final String RPC_METHODS = "rpc-methods";
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
     * Parses the sync mode from command line arguments.
     * <p>
     * Defaults to "warp" if not specified. Throws an exception for invalid values.
     *
     * @param cmd Command line arguments.
     * @return The selected sync mode.
     * @throws CliArgsParseException for invalid sync mode values.
     */
    @NotNull
    private static SyncMode parseSyncMode(CommandLine cmd) {
        try {
            return SyncMode.valueOf(cmd.getOptionValue(SYNC_MODE, "warp").toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CliArgsParseException("Invalid sync mode provided, valid values - WARP or FULL", e);
        }
    }

    /**
     * Parses the RPC methods setting from command line arguments.
     * <p>
     * Defaults to "auto" if not specified. Throws an exception for invalid values.
     *
     * @param cmd Command line arguments.
     * @return The selected RPC methods setting.
     * @throws CliArgsParseException for invalid RPC methods values.
     */
    @NotNull
    private static RpcMethods parseRpcMethods(CommandLine cmd) {
        try {
            return RpcMethods.valueOf(cmd.getOptionValue(RPC_METHODS, "auto").toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new CliArgsParseException("Invalid rpc methods provided, valid values - AUTO, SAFE, UNSAFE", e);
        }
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
            SyncMode syncMode = parseSyncMode(cmd);
            RpcMethods rpcMethods = parseRpcMethods(cmd);
            boolean isPublic = cmd.hasOption(PUBLIC_RPC);

            boolean unsafeEnabled = isPublic ? rpcMethods == RpcMethods.UNSAFE : rpcMethods != RpcMethods.SAFE;

            return new CliArguments(network, dbPath, dbRecreate, nodeKey, nodeMode, noLgacyProtocols, syncMode,
                    unsafeEnabled);
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
        Option nodeMode = new Option("mode", NODE_MODE, true, "\nNode mode (light/full). " +
                                                              "Full by default.");
        Option noLegacyProtocols = new Option(null, NO_LEGACY_PROTOCOLS, false,
                "\nDoesn't use legacy protocols if set");
        Option syncMode = new Option(null, SYNC_MODE, true,
                "\nSync mode (warp/full) - warp by default");
        Option publicRpc = new Option(null, PUBLIC_RPC, false,
                "\nListen to all RPC interfaces (by default only local interface is listened on).");
        Option rpcMethods = new Option(null, RPC_METHODS, false, """
                                
                RPC methods to expose.

                [default: auto]

                Possible values:
                - auto:   Expose every RPC method only when RPC is listening on
                  `localhost`, otherwise serve only safe RPC methods
                - safe:   Allow only a safe subset of RPC methods
                - unsafe: Expose every RPC method (even potentially unsafe ones)""");
        networkOption.setRequired(false);
        dbPathOption.setRequired(false);
        dbClean.setRequired(false);
        nodeKey.setRequired(false);
        nodeMode.setRequired(false);
        noLegacyProtocols.setRequired(false);
        syncMode.setRequired(false);
        publicRpc.setRequired(false);
        rpcMethods.setRequired(false);

        result.addOption(networkOption);
        result.addOption(dbPathOption);
        result.addOption(dbClean);
        result.addOption(nodeKey);
        result.addOption(nodeMode);
        result.addOption(noLegacyProtocols);
        result.addOption(syncMode);
        result.addOption(publicRpc);
        result.addOption(rpcMethods);
        return result;
    }

}

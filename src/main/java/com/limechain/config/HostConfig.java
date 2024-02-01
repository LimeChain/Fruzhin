package com.limechain.config;

import com.limechain.chain.Chain;
import com.limechain.cli.CliArguments;
import com.limechain.constants.RpcConstants;
import com.limechain.exception.InvalidChainException;
import com.limechain.exception.InvalidNodeRoleException;
import com.limechain.network.protocol.blockannounce.NodeRole;
import lombok.Getter;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Value;

import java.util.logging.Level;

import static com.limechain.chain.Chain.WESTEND;
import static com.limechain.chain.Chain.fromString;

/**
 * Configuration class used to store any Host specific information
 */
@Log
@Getter
public class HostConfig {
    /**
     * File path under which the DB will be created
     */
    private final String rocksDbPath;
    /**
     * Chain the Host is running on
     */
    private final Chain chain;
    private final NodeRole nodeRole;
    private final String rpcNodeAddress;

    // TODO:
    //  Think about how to avoid the need for this (reordering in the bean initialization necessary).
    //  we can use the autowired org.springframework.core.env.Environment to dynamically load properties
    //  but it's not yet available during construction of this bean.
    @Value("${genesis.path.polkadot}")
    private String polkadotGenesisPath;
    @Value("${genesis.path.kusama}")
    private String kusamaGenesisPath;
    @Value("${genesis.path.westend}")
    private String westendGenesisPath;
    @Value("${genesis.path.local}")
    private String localGenesisPath;

    /**
     * Recreate the DB
     */
    private final boolean dbRecreate;

    public HostConfig(CliArguments cliArguments) {
        this.rocksDbPath = cliArguments.dbPath();
        this.dbRecreate = cliArguments.dbRecreate();

        String network = cliArguments.network();
        Chain chain = network.isEmpty() ? WESTEND : fromString(network);
        if (chain == null) {
            throw new InvalidChainException(String.format("\"%s\" is not a valid chain.", network));
        }
        this.chain = chain;

        NodeRole nodeRole = NodeRole.fromString(cliArguments.nodeRole());
        if (nodeRole == null) {
            throw new InvalidNodeRoleException(String.format("\"%s\" is not a valid node role.", cliArguments.nodeRole()));
        }
        this.nodeRole = nodeRole;

        this.rpcNodeAddress = switch (chain) {
            case POLKADOT, LOCAL -> RpcConstants.POLKADOT_WS_RPC;
            case KUSAMA -> RpcConstants.KUSAMA_WS_RPC;
            case WESTEND -> RpcConstants.WESTEND_WS_RPC;
        };

        log.log(Level.INFO, String.format("✅️Loaded app config for chain %s%n", chain));
    }

    /**
     * Gets the genesis file path based on the chain the node is configured
     *
     * @return genesis(chain spec) file path
     */
    public String getGenesisPath() {
        return switch (chain) {
            case POLKADOT -> polkadotGenesisPath;
            case KUSAMA -> kusamaGenesisPath;
            case WESTEND -> westendGenesisPath;
            case LOCAL -> localGenesisPath;
        };
    }
}

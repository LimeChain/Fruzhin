package com.limechain.config;

import com.limechain.chain.Chain;
import com.limechain.cli.CliArguments;
import com.limechain.constants.RpcConstants;
import com.limechain.exception.InvalidChainException;
import com.limechain.exception.InvalidNodeRoleException;
import com.limechain.network.protocol.blockannounce.NodeRole;
import lombok.Getter;
import lombok.Setter;
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
@Setter
public class HostConfig {
    /**
     * File path under which the DB will be created
     */
    private String rocksDbPath;
    /**
     * Chain the Host is running on
     */
    private Chain chain;
    private NodeRole nodeRole;
    private String rpcNodeAddress;
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
    private boolean dbRecreate;

    public HostConfig(CliArguments cliArguments) {
        this.setRocksDbPath(cliArguments.dbPath());
        String network = cliArguments.network();
        this.setChain(network.isEmpty() ? WESTEND : fromString(network));
        if (chain == null) {
            throw new InvalidChainException("Chain cannot be null");
        }
        dbRecreate = cliArguments.dbRecreate();
        nodeRole = NodeRole.fromString(cliArguments.nodeRole());
        if (nodeRole == null) throw new InvalidNodeRoleException();
        log.log(Level.INFO, String.format("✅️Loaded app config for chain %s%n", chain));

        switch (this.getChain()) {
            case POLKADOT, LOCAL -> this.setRpcNodeAddress(RpcConstants.POLKADOT_WS_RPC);
            case KUSAMA -> this.setRpcNodeAddress(RpcConstants.KUSAMA_WS_RPC);
            case WESTEND -> this.setRpcNodeAddress(RpcConstants.WESTEND_WS_RPC);
        }
    }

    /**
     * Gets the genesis file path based on the chain the node is configured
     *
     * @return genesis(chain spec) file path
     * @throws InvalidChainException if chain is invalid.
     *                          This shouldn't be possible in practice because of preceding validations.
     */
    public String getGenesisPath() {
        switch (chain) {
            case POLKADOT -> {
                return polkadotGenesisPath;
            }
            case KUSAMA -> {
                return kusamaGenesisPath;
            }
            case WESTEND -> {
                return westendGenesisPath;
            }
            case LOCAL -> {
                return localGenesisPath;
            }
            default -> throw new InvalidChainException(String.format("\"%s\" is not a valid chain", chain.getValue()));
        }
    }
}

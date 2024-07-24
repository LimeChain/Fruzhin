package com.limechain.config;

import com.limechain.chain.Chain;
import com.limechain.constants.RpcConstants;
import com.limechain.exception.misc.InvalidChainException;
import com.limechain.network.protocol.blockannounce.NodeRole;
import lombok.Getter;
import lombok.extern.java.Log;

import java.util.Optional;
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
     * Chain the Host is running on
     */
    private final Chain chain;
    private final NodeRole nodeRole;
    private final String rpcNodeAddress;

    private String polkadotGenesisPath = "genesis/polkadot.json";
    private String kusamaGenesisPath = "genesis/kusama.json";
    private String westendGenesisPath = "genesis/westend.json";
    private String localGenesisPath = "genesis/local.json";

    public HostConfig() {
        String network = "polkadot";
        this.chain = Optional
                .ofNullable(network.isEmpty() ? WESTEND : fromString(network))
                .orElseThrow(() -> new InvalidChainException(String.format("\"%s\" is not a valid chain.", network)));

        this.nodeRole = NodeRole.LIGHT;

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

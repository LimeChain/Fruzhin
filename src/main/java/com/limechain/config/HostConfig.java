package com.limechain.config;

import com.limechain.chain.Chain;
import com.limechain.constants.RpcConstants;
import com.limechain.utils.DivLogger;
import lombok.Getter;

import java.util.logging.Level;

/**
 * Configuration class used to store any Host specific information
 */
@Getter
public class HostConfig {
    /**
     * Chain the Host is running on
     */
    private final Chain chain;
    //    private final NodeRole nodeRole;
    private final String rpcNodeAddress;

    private String polkadotGenesisPath = "genesis/polkadot.json";
    private String kusamaGenesisPath = "genesis/kusama.json";
    private String westendGenesisPath = "genesis/westend.json";
    private String localGenesisPath = "genesis/local.json";

    private static final DivLogger log = new DivLogger();

    public HostConfig() {
        log.log(Level.INFO, "Loading app config...");
        this.chain = Chain.POLKADOT;
//        Optional
//                .ofNullable(network.isEmpty() ? WESTEND : fromString(network))
//                .orElseThrow(() -> new InvalidChainException(String.format("\"%s\" is not a valid chain.", network)));

//        this.nodeRole = NodeRole.LIGHT;

        log.log(Level.INFO, "Loading rpcNodeAddress...");
        switch (chain.getValue()) {
            case "POLKADOT", "LOCAL":
                rpcNodeAddress = RpcConstants.POLKADOT_WS_RPC;
                break;
            case "KUSAMA":
                rpcNodeAddress = RpcConstants.KUSAMA_WS_RPC;
                break;
            case "WESTEND":
                rpcNodeAddress = RpcConstants.WESTEND_WS_RPC;
                break;
            default:
                rpcNodeAddress = RpcConstants.POLKADOT_WS_RPC;
        }

        log.log(Level.INFO, "✅️Loaded app config for chain " + chain);
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

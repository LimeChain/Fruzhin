package com.limechain.config;

import com.limechain.chain.Chain;
import com.limechain.cli.CliArguments;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.logging.Level;

import static com.limechain.chain.Chain.WESTEND;
import static com.limechain.chain.Chain.fromString;

@Log
@Getter
@Setter
public class HostConfig {
    private String rocksDbPath;
    private Chain chain;
    //TODO Make bootstrapNodes not hardcoded
    @Value("${genesis.path.polkadot}")
    private String polkadotGenesisPath;
    @Value("${genesis.path.kusama}")
    private String kusamaGenesisPath;
    @Value("${genesis.path.westend}")
    private String westendGenesisPath;
    @Value("${genesis.path.local}")
    private String localGenesisPath;
    @Value("${helper.node.address}")
    private String helperNodeAddress;

    public HostConfig(CliArguments cliArguments) {
        this.setRocksDbPath(cliArguments.dbPath());
        String network = cliArguments.network();
        this.setChain(network.isEmpty() ? WESTEND : fromString(network));
        if (chain == null) {
            throw new RuntimeException("Unsupported or unknown network");
        }
        log.log(Level.INFO, String.format("✅️Loaded app config for chain %s%n", chain));
    }

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
            case LOCAL ->{
                return localGenesisPath;
            }
            default -> {
                throw new RuntimeException("Invalid Chain in host configuration");
            }
        }
    }
}

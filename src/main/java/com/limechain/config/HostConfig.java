package com.limechain.config;

import com.limechain.chain.Chain;
import com.limechain.cli.CliArguments;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Value;

import java.util.logging.Level;

import static com.limechain.chain.Chain.*;

@Log
@Getter
@Setter
public class HostConfig {
    private String rocksDbPath;
    private Chain chain;

    @Value("${genesis.path.polkadot}")
    private String polkadotGenesisPath;
    @Value("${genesis.path.kusama}")
    private String kusamaGenesisPath;
    @Value("${genesis.path.westend}")
    private String westendGenesisPath;
    @Value("${helper.node.address}")
    private String helperNodeAddress;


    public HostConfig(CliArguments cliArguments) {
        this.setRocksDbPath(cliArguments.dbPath());
        String network = cliArguments.network();
        chain = network.isEmpty()? WESTEND : fromString(network);
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
            default -> {
                throw new RuntimeException("Invalid Chain in host configuration");
            }
        }
    }
}

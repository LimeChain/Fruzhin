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
    private String genesisPath;
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
        boolean isNetworkSet = setMatchedNetwork(cliArguments.network());
        if (!isNetworkSet) {
            throw new RuntimeException("Unsupported or unknown network");
        }
        log.log(Level.INFO, String.format("✅️Loaded app config for chain %s%n", chain));
    }

    private boolean setMatchedNetwork(String network) {
        if (network.equals(POLKADOT.getValue())) {
            this.setGenesisPath(polkadotGenesisPath);
            this.setChain(POLKADOT);
            return true;
        }
        if (network.equals(KUSAMA.getValue())) {
            this.setGenesisPath(kusamaGenesisPath);
            this.setChain(Chain.KUSAMA);
            return true;
        }
        // Empty string case because we want the default network to be Westend
        if (network.equals(WESTEND.getValue()) || network.isEmpty()) {
            this.setGenesisPath(westendGenesisPath);
            this.setChain(Chain.WESTEND);
            return true;
        }
        return false;
    }
}

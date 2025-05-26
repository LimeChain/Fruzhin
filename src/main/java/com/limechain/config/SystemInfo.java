package com.limechain.config;

import com.limechain.chain.Chain;
import com.limechain.network.NetworkService;
import com.limechain.sync.state.SyncState;
import lombok.Getter;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigInteger;
import java.nio.file.FileSystems;

/**
 * Configuration class used to hold and information used by the system rpc methods
 */
@Getter
@Log
public class SystemInfo {
    private final String role;
    private final Chain chain;
    private final String dbPath;
    private final String hostIdentity;
    @Value("${host.name}")
    private String hostName;
    @Value("${host.version}")
    private String hostVersion;
    private final BigInteger highestBlock;

    public SystemInfo(HostConfig hostConfig, NetworkService network, SyncState syncState) {
        this.role = network.getNodeRole().name();
        this.chain = hostConfig.getChain();
        this.dbPath = hostConfig.getRocksDbPath();
        this.hostIdentity = network.getHost().getPeerId().toString();
        this.highestBlock = syncState.getLastFinalizedBlockNumber();
    }

    /**
     * Logs system info on node startup
     */
    public void logSystemInfo() {
        String lemonEmoji = new String(Character.toChars(0x1F34B));
        String pinEmoji = new String(Character.toChars(0x1F4CC));
        String clipboardEmoji = new String(Character.toChars(0x1F4CB));
        String labelEmoji = new String(Character.toChars(0x1F3F7));
        String authEmoji = new String(Character.toChars(0x1F464));
        String floppyEmoji = new String(Character.toChars(0x1F4BE));
        String absoluteDbPath = FileSystems.getDefault().getPath(dbPath).normalize().toAbsolutePath().toString();

        log.info(String.format("%s LimeChain Fruzhin", lemonEmoji));
        log.info(String.format("%s Version: %s", pinEmoji, hostVersion));
        log.info(String.format("%s Chain specification: %s", clipboardEmoji, chain.getValue()));
        log.info(String.format("%s Host name: %s", labelEmoji, hostName));
        log.info(String.format("%s Role: %s", authEmoji, role));
        log.info(String.format("%s Database: RocksDb at %s", floppyEmoji, absoluteDbPath));
        log.info(String.format("Local node identity is: %s", hostIdentity));
        log.info(String.format("Operating System: %s", System.getProperty("os.name")));
        log.info(String.format("CPU architecture: %s", System.getProperty("os.arch")));

        BigInteger effectiveHighestBlock = this.highestBlock != null ? this.highestBlock : BigInteger.ZERO;
        log.info(String.format("Highest known block at #%d", effectiveHighestBlock));
    }
}

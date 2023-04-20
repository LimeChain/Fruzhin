package com.limechain.config;

import com.limechain.chain.Chain;
import com.limechain.network.Network;
import lombok.Getter;
import lombok.extern.java.Log;
import org.springframework.beans.factory.annotation.Value;

import java.nio.file.FileSystems;
import java.util.logging.Level;

/**
 * Configuration class used to hold and information used by the system rpc methods
 */
@Getter
@Log
public class SystemInfo {
    private final String role;
    @Value("${host.name}")
    private String hostName;
    @Value("${host.version}")
    private String hostVersion;
    private Chain chain;
    private String dbPath;
    private String hostIdentity;

    public SystemInfo(HostConfig hostConfig, Network network) {
        // TODO: In the future this will be set depending on CLI params
        this.role = "LightClient";
        this.chain = hostConfig.getChain();
        this.dbPath = hostConfig.getRocksDbPath();
        this.hostIdentity = network.getHost().getPeerId().toString();
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

        log.log(Level.INFO, lemonEmoji + "LimeChain Fruzhin");
        log.log(Level.INFO, pinEmoji + "Version: " + hostVersion);
        log.log(Level.INFO, clipboardEmoji + "Chain specification: " + chain.getValue());
        log.log(Level.INFO, labelEmoji + "Host name: " + hostName);
        log.log(Level.INFO, authEmoji + "Role: " + role);
        log.log(Level.INFO, floppyEmoji + "Database: RocksDb at " + absoluteDbPath);
        log.log(Level.INFO, "Local node identity is: " + hostIdentity);
        log.log(Level.INFO, "Operating System: " + System.getProperty("os.name"));
        log.log(Level.INFO, "CPU architecture: " + System.getProperty("os.arch"));

        //TODO Get highest known block from database
        log.log(Level.INFO, "Highest known block at #0");
    }
}

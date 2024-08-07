package com.limechain.config;

import com.limechain.chain.Chain;
import com.limechain.storage.block.SyncState;
import com.limechain.utils.DivLogger;
import lombok.Getter;

import java.math.BigInteger;
import java.util.logging.Level;

/**
 * Configuration class used to hold and information used by the system rpc methods
 */
@Getter
public class SystemInfo {
//    private final String role;
    private final Chain chain;
//    private final String hostIdentity;
    private String hostName = "Fruzhin";
    private String hostVersion = "0.0.1";
    private final BigInteger highestBlock;

    private static final DivLogger log = new DivLogger();

    public SystemInfo(HostConfig hostConfig, SyncState syncState) {
        log.log("Building SystemInfo constructor");
//        this.role = network.getNodeRole().name();
        this.chain = hostConfig.getChain();
        log.log("getChain");
//        this.hostIdentity = network.getHost().getPeerId().toString();
        this.highestBlock = syncState.getLastFinalizedBlockNumber();

        log.log("SystemInfo built");
        logSystemInfo();
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

        log.log(Level.INFO, lemonEmoji + "LimeChain Fruzhin");
        log.log(Level.INFO, pinEmoji + "Version: " + hostVersion);
        log.log(Level.INFO, clipboardEmoji + "Chain specification: " + chain.getValue());
        log.log(Level.INFO, labelEmoji + "Host name: " + hostName);
//        log.log(Level.INFO, authEmoji + "Role: " + role);
//        log.log(Level.INFO, "Local node identity is: " + hostIdentity);
        log.log(Level.INFO, "Operating System: " + System.getProperty("os.name"));
        log.log(Level.INFO, "CPU architecture: " + System.getProperty("os.arch"));
        log.log(Level.INFO, "Highest known block at #" + highestBlock);
    }
}

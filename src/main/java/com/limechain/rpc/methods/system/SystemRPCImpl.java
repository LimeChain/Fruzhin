package com.limechain.rpc.methods.system;

import com.limechain.chain.ChainService;
import com.limechain.config.SystemInfo;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class SystemRPCImpl {

    private final ChainService chainService;
    private final SystemInfo systemInfo;

    public SystemRPCImpl (ChainService chainService, SystemInfo systemInfo) {
        this.chainService = chainService;
        this.systemInfo = systemInfo;
    }

    public String systemName () {
        return this.systemInfo.hostName;
    }

    public String systemVersion () {
        return this.systemInfo.hostVersion;
    }

    public String systemChain () {
        return this.chainService.genesis.name;
    }

    public String systemChainType () {
        return this.chainService.genesis.chainType;
    }

    public Map<String, Object> systemProperties () {
        return this.chainService.genesis.properties;
    }

    public String[] systemNodeRoles () {
        return new String[]{this.systemInfo.role};
    }

    // TODO: Implement in M2.
    public Map<String, Object> systemHealth () {
        return null;
    }

    // TODO: Implement in M2.
    public String systemLocalPeerId () {
        return null;
    }

    // TODO: Implement in M2.
    public String[] systemLocalListenAddress () {
        return new String[0];
    }

    // TODO: Implement in M2.
    public String[] systemSystemPeers () {
        return new String[0];
    }

    // TODO: Implement in M2.
    public String systemAddReservedPeer (String peerId) {
        return null;
    }

    // TODO: Implement in M2.
    public String systemRemoveReservedPeer (String peerId) {
        return null;
    }

    // TODO: Implement in M2.
    public Map<String, Object> systemSyncState () {
        return null;
    }

    // TODO: Implement in M2.
    public String systemAccountNextIndex (String accountAddress) {
        return null;
    }

    // TODO: Implement in M2.
    public String systemDryRun (String extrinsic, String blockHash) {
        return null;
    }

}

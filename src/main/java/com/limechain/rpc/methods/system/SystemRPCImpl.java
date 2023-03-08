package com.limechain.rpc.methods.system;

import com.limechain.chain.ChainService;
import com.limechain.config.SystemInfo;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@AllArgsConstructor
public class SystemRPCImpl {

    private final ChainService chainService;
    private final SystemInfo systemInfo;

    public String systemName () {
        return this.systemInfo.getHostName();
    }

    public String systemVersion () {
        return this.systemInfo.getHostVersion();
    }

    public String systemChain () {
        return this.chainService.getGenesis().getName();
    }

    public String systemChainType () {
        return this.chainService.getGenesis().getChainType();
    }

    public Map<String, Object> systemProperties () {
        return this.chainService.getGenesis().getProperties();
    }

    public String[] systemNodeRoles () {
        return new String[]{this.systemInfo.getRole()};
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

package com.limechain.rpc.methods;

import com.googlecode.jsonrpc4j.spring.AutoJsonRpcServiceImpl;
import com.limechain.rpc.methods.chain.ChainRPCImpl;
import com.limechain.rpc.methods.system.SystemRPCImpl;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@AutoJsonRpcServiceImpl
@AllArgsConstructor
public class RPCMethodsImpl implements RPCMethods {
    private final SystemRPCImpl systemRPC;
    private final ChainRPCImpl chainRPC;

    @Override
    public String systemName () {
        return systemRPC.systemName();
    }

    @Override
    public String systemVersion () {
        return systemRPC.systemVersion();
    }

    @Override
    public String systemChain () {
        return systemRPC.systemChain();
    }

    @Override
    public String systemChainType () {
        return systemRPC.systemChainType();
    }

    @Override
    public Map<String, Object> systemProperties () {
        return systemRPC.systemProperties();
    }

    @Override
    public String[] systemNodeRoles () {
        return systemRPC.systemNodeRoles();
    }

    @Override
    public Map<String, Object> systemHealth () {
        return systemRPC.systemHealth();
    }

    @Override
    public String systemLocalPeerId () {
        return systemRPC.systemLocalPeerId();
    }

    @Override
    public String[] systemLocalListenAddress () {
        return systemRPC.systemLocalListenAddress();
    }

    @Override
    public String[] systemSystemPeers () {
        return systemRPC.systemSystemPeers();
    }

    @Override
    public String systemAddReservedPeer (String peerId) {
        return systemRPC.systemAddReservedPeer(peerId);
    }

    @Override
    public String systemRemoveReservedPeer (String peerId) {
        return systemRPC.systemRemoveReservedPeer(peerId);
    }

    @Override
    public Map<String, Object> systemSyncState () {
        return systemRPC.systemSyncState();
    }

    @Override
    public String systemAccountNextIndex (String accountAddress) {
        return systemRPC.systemAccountNextIndex(accountAddress);
    }

    @Override
    public String systemDryRun (String extrinsic, String blockHash) {
        return systemRPC.systemDryRun(extrinsic, blockHash);
    }

    @Override
    public String[] rpcMethods () {
        //TODO: Use reflection in order to not hard code
        return new String[0];
    }

    @Override
    public void chainUnstableFollow () {
        chainRPC.chainUnstableFollow();
    }

    @Override
    public String chainUnstableUnfollow () {
        return null;
    }

    @Override
    public String chainUnstableUnpin () {
        return null;
    }

    @Override
    public String chainUnstableStorage () {
        return null;
    }

    @Override
    public String chainUnstableCall () {
        return null;
    }

    @Override
    public String chainUnstableStopCall () {
        return null;
    }

    @Override
    public String transactionUnstableSubmitAndWatch () {
        return null;
    }
}

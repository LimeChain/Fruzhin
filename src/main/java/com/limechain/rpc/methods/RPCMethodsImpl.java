package com.limechain.rpc.methods;

import com.googlecode.jsonrpc4j.JsonRpcMethod;
import com.googlecode.jsonrpc4j.spring.AutoJsonRpcServiceImpl;
import com.limechain.rpc.methods.system.SystemRPC;
import com.limechain.rpc.methods.system.SystemRPCImpl;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Implementation class for each rpc method. This class will become very large in the future
 * when we support more rpc methods, however due to the limitations of jsonrpc4j described in {@link RPCMethods},
 * there's nothing we can do about splitting up the class into several ones for now
 */
@Service
@AutoJsonRpcServiceImpl
@AllArgsConstructor
public class RPCMethodsImpl implements RPCMethods {
    
    /**
     * References to rpc method implementation classes
     */
    private final SystemRPCImpl systemRPC;

    @Override
    public String systemName() {
        return systemRPC.systemName();
    }

    @Override
    public String systemVersion() {
        return systemRPC.systemVersion();
    }

    @Override
    public String systemChain() {
        return systemRPC.systemChain();
    }

    @Override
    public String systemChainType() {
        return systemRPC.systemChainType();
    }

    @Override
    public Map<String, Object> systemProperties() {
        return systemRPC.systemProperties();
    }

    @Override
    public String[] systemNodeRoles() {
        return systemRPC.systemNodeRoles();
    }

    @Override
    public Map<String, Object> systemHealth() {
        return systemRPC.systemHealth();
    }

    @Override
    public String systemLocalPeerId() {
        return systemRPC.systemLocalPeerId();
    }

    @Override
    public String[] systemLocalListenAddress() {
        return systemRPC.systemLocalListenAddress();
    }

    @Override
    public List<Map<String, String>> systemSystemPeers() {
        return systemRPC.systemSystemPeers();
    }

    @Override
    public void systemAddReservedPeer(String peerId) {
        systemRPC.systemAddReservedPeer(peerId);
    }

    @Override
    public void systemRemoveReservedPeer(String peerId) {
        systemRPC.systemRemoveReservedPeer(peerId);
    }

    @Override
    public Map<String, Object> systemSyncState() {
        return systemRPC.systemSyncState();
    }

    @Override
    public String systemAccountNextIndex(String accountAddress) {
        return systemRPC.systemAccountNextIndex(accountAddress);
    }

    @Override
    public String systemDryRun(String extrinsic, String blockHash) {
        return systemRPC.systemDryRun(extrinsic, blockHash);
    }

    @Override
    public String[] rpcMethods() {
        ArrayList<Method> methods = new ArrayList<>();

        Collections.addAll(methods, RPCMethods.class.getDeclaredMethods());
        Collections.addAll(methods, SystemRPC.class.getDeclaredMethods());

        return methods.stream().map(m -> m.getAnnotation(JsonRpcMethod.class).value()).toArray(String[]::new);
    }
}

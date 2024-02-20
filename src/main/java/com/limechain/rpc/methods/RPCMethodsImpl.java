package com.limechain.rpc.methods;

import com.googlecode.jsonrpc4j.JsonRpcMethod;
import com.googlecode.jsonrpc4j.spring.AutoJsonRpcServiceImpl;
import com.limechain.chain.spec.ChainSpec;
import com.limechain.chain.spec.ChainType;
import com.limechain.chain.spec.PropertyValue;
import com.limechain.rpc.methods.chain.ChainRPCImpl;
import com.limechain.rpc.methods.sync.SyncRPCImpl;
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
     * References to system rpc method implementation classes
     */
    private final SystemRPCImpl systemRPC;

    /**
     * References to sync rpc method implementation classes
     */
    private final SyncRPCImpl syncRPC;

    /**
     * References to chain rpc method implementation classes
     */
    private final ChainRPCImpl chainRPC;

    @Override
    public String[] rpcMethods() {
        ArrayList<Method> methods = new ArrayList<>();

        Collections.addAll(methods, RPCMethods.class.getDeclaredMethods());
        Collections.addAll(methods, SystemRPC.class.getDeclaredMethods());

        return methods.stream().map(m -> m.getAnnotation(JsonRpcMethod.class).value()).toArray(String[]::new);
    }

    //region SystemRPC methods
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
    public ChainType systemChainType() {
        return systemRPC.systemChainType();
    }

    @Override
    public Map<String, PropertyValue> systemProperties() {
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
    public List<Map<String, Object>> systemSystemPeers() {
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
    //endregion

    //region SyncRPC methods
    @Override
    public ChainSpec syncStateGenSyncSpec(boolean raw) {
        return syncRPC.syncStateGetSyncSpec(raw);
    }
    //endregion

    //region ChainRPC methods
    @Override
    public Map<String, Object> chainGetHeader(final String blockHashArg) {
        return this.chainRPC.chainGetHeader(blockHashArg);
    }

    @Override
    public Map<String, Object> chainGetBlock(final String blockHash) {
        return this.chainRPC.chainGetBlock(blockHash);
    }

    @Override
    public Object chainGetBlockHash(final Object... blockNumbers) {
        return this.chainRPC.chainGetBlockHash(blockNumbers);
    }

    @Override
    public Object chainGetHead(final Object... blockNumbers) {
        return chainGetBlockHash(blockNumbers);
    }

    @Override
    public String chainGetFinalizedHead() {
        return this.chainRPC.chainGetFinalizedHead();
    }

    @Override
    public String chainGetFinalisedHead() {
        return chainGetFinalizedHead();
    }

    @Override
    public String chainSubscribeAllHeads() {
        return null;
    }

    @Override
    public String chainUnsubscribeAllHeads() {
        return null;
    }

    @Override
    public String chainSubscribeNewHeads() {
        return null;
    }

    @Override
    public String chainUnsubscribeNewHeads() {
        return null;
    }

    @Override
    public String chainSubscribeFinalizedHeads() {
        return null;
    }

    @Override
    public String chainUnsubscribeFinalizedHeads() {
        return null;
    }
    //endregion
}

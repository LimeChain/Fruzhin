package com.limechain.rpc.methods.system;

import com.googlecode.jsonrpc4j.JsonRpcMethod;
import com.limechain.chain.spec.ChainType;
import com.limechain.chain.spec.PropertyValue;

import java.util.List;
import java.util.Map;

/**
 * Interface which holds all system rpc methods and their interfaces
 *
 * @JsonRpcMethod Sets the method name used whenever jsonrpc request is received
 */
public interface SystemRPC {
    @JsonRpcMethod("system_name")
    String systemName();

    @JsonRpcMethod("system_version")
    String systemVersion();

    @JsonRpcMethod("system_chain")
    String systemChain();

    @JsonRpcMethod("system_chainType")
    ChainType systemChainType();

    @JsonRpcMethod("system_properties")
    Map<String, PropertyValue> systemProperties();

    @JsonRpcMethod("system_nodeRoles")
    String[] systemNodeRoles();

    @JsonRpcMethod("system_health")
    Map<String, Object> systemHealth();

    @JsonRpcMethod("system_localPeerId")
    String systemLocalPeerId();

    @JsonRpcMethod("system_localListenAddress")
    String[] systemLocalListenAddress();

    @JsonRpcMethod("system_peers")
    List<Map<String, Object>> systemSystemPeers();

    @JsonRpcMethod("system_addReservedPeer")
    void systemAddReservedPeer(String peerId);

    @JsonRpcMethod("system_removeReservedPeer")
    void systemRemoveReservedPeer(String peerId);

    @JsonRpcMethod("system_syncState")
    Map<String, Object> systemSyncState();

    @JsonRpcMethod("system_accountNextIndex")
    String systemAccountNextIndex(String accountAddress);

    @JsonRpcMethod("system_dryRun")
    String systemDryRun(String extrinsic, String blockHash);

}

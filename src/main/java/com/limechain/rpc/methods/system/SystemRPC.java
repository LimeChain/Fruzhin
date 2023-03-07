package com.limechain.rpc.methods.system;

import com.googlecode.jsonrpc4j.JsonRpcMethod;

import java.util.Map;

public interface SystemRPC {
    @JsonRpcMethod("system_name")
    String systemName ();

    @JsonRpcMethod("system_version")
    String systemVersion ();

    @JsonRpcMethod("system_chain")
    String systemChain ();

    @JsonRpcMethod("system_chainType")
    String systemChainType ();

    //TODO: Change return type to be specific class
    @JsonRpcMethod("system_properties")
    Map<String, Object> systemProperties ();

    @JsonRpcMethod("system_nodeRoles")
    String[] systemNodeRoles ();

    @JsonRpcMethod("system_health")
    Map<String, Object> systemHealth ();

    @JsonRpcMethod("system_localPeerId")
    String systemLocalPeerId ();

    @JsonRpcMethod("system_localListenAddress")
    String[] systemLocalListenAddress ();

    @JsonRpcMethod("system_peers")
    String[] systemSystemPeers ();

    @JsonRpcMethod("system_addReservedPeer")
    String systemAddReservedPeer (String peerId);

    @JsonRpcMethod("system_removeReservedPeer")
    String systemRemoveReservedPeer (String peerId);

    @JsonRpcMethod("system_syncState")
    Map<String, Object> systemSyncState ();

    @JsonRpcMethod("system_accountNextIndex")
    String systemAccountNextIndex (String accountAddress);

    @JsonRpcMethod("system_dryRun")
    String systemDryRun (String extrinsic, String blockHash);

}

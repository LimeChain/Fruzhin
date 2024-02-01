package com.limechain.rpc.methods.sync;

import com.googlecode.jsonrpc4j.JsonRpcMethod;
import com.limechain.chain.spec.RawChainSpec;

/**
 * Interface which holds all sync rpc methods and their interfaces
 *
 * @JsonRpcMethod Sets the method name used whenever jsonrpc request is received
 */
public interface SyncRPC {

    @JsonRpcMethod("sync_state_genSyncSpec")
    RawChainSpec syncStateGenSyncSpec(boolean raw);

}

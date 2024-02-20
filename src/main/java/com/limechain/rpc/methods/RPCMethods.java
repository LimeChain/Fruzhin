package com.limechain.rpc.methods;

import com.googlecode.jsonrpc4j.JsonRpcMethod;
import com.googlecode.jsonrpc4j.JsonRpcService;
import com.limechain.rpc.methods.chain.ChainRPC;
import com.limechain.rpc.methods.offchain.OffchainRPC;
import com.limechain.rpc.methods.sync.SyncRPC;
import com.limechain.rpc.methods.system.SystemRPC;

/**
 * Interface that serves as a jsonrpc method family(namespace) aggregator.
 * <p>
 * Unfortunately @JsonRpcService, doesn't allow us to have multiple separate interfaces under one route("/").
 * Instead, it will overwrite each new one resulting in only one interface being mapped to the route.
 * Therefore, as a workaround, we have to combine them into a single interface 🤷
 */
@JsonRpcService("/")
public interface RPCMethods extends SystemRPC, SyncRPC, ChainRPC, OffchainRPC {
    @JsonRpcMethod("rpc_methods")
    String[] rpcMethods();

}

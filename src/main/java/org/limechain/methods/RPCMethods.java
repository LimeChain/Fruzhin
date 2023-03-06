package org.limechain.methods;

import com.googlecode.jsonrpc4j.JsonRpcMethod;
import com.googlecode.jsonrpc4j.JsonRpcService;
import org.limechain.methods.chain.ChainRPC;
import org.limechain.methods.system.SystemRPC;
import org.limechain.methods.transaction.TransactionRPC;

// Unfortunately @JsonRpcService, doesn't allow us to have multiple separate interfaces under one route.
// Instead, it will overwrite each new one resulting in only one interface being mapped to the endpoint
// Therefore, we have to combine them into a single interface 🤷
@JsonRpcService("/")
public interface RPCMethods extends SystemRPC, ChainRPC, TransactionRPC {
    @JsonRpcMethod("rpc_methods")
    String[] rpcMethods ();

}

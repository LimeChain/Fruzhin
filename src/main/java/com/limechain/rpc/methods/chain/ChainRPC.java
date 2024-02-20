package com.limechain.rpc.methods.chain;

import com.googlecode.jsonrpc4j.JsonRpcMethod;

import java.util.Map;

/**
 * Interface which holds all chain rpc methods and their interfaces
 *
 * @JsonRpcMethod Sets the method name used whenever jsonrpc request is received
 */
public interface ChainRPC {

    @JsonRpcMethod("chain_getHeader")
    Map<String, Object> chainGetHeader(String blockHash);

    @JsonRpcMethod("chain_getBlock")
    Map<String, Object> chainGetBlock(String blockHash);

    @JsonRpcMethod("chain_getBlockHash")
    Object chainGetBlockHash(Object... blockNumbers);

    @JsonRpcMethod("chain_getHead") //Alias for chain_getBlockHash
    Object chainGetHead(Object... blockNumbers);

    @JsonRpcMethod("chain_getFinalizedHead")
    String chainGetFinalizedHead();

    @JsonRpcMethod("chain_getFinalisedHead") //Alias for chain_getFinalizedHead
    String chainGetFinalisedHead();

    @JsonRpcMethod("chain_subscribeAllHeads")
    String chainSubscribeAllHeads();

    @JsonRpcMethod("chain_unsubscribeAllHeads")
    String chainUnsubscribeAllHeads();

    @JsonRpcMethod("chain_subscribeNewHeads")
    String chainSubscribeNewHeads();

    @JsonRpcMethod("chain_unsubscribeNewHeads")
    String chainUnsubscribeNewHeads();

    @JsonRpcMethod("chain_subscribeFinalizedHeads")
    String chainSubscribeFinalizedHeads();

    @JsonRpcMethod("chain_unsubscribeFinalizedHeads")
    String chainUnsubscribeFinalizedHeads();

}

package com.limechain.rpc.methods.offchain;

import com.googlecode.jsonrpc4j.JsonRpcMethod;

public interface OffchainRPC {
    @JsonRpcMethod("offchain_localStorageSet")
    void offchainLocalStorageSet(final String storageKind, final String key, final String value);

    @JsonRpcMethod("offchain_localStorageGet")
    String ofchainLocalStorageGet(final String storageKind, final String key);

}

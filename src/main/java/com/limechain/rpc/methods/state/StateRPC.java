package com.limechain.rpc.methods.state;

import com.googlecode.jsonrpc4j.JsonRpcMethod;
import com.limechain.rpc.config.UnsafeRpcMethod;
import com.limechain.rpc.methods.state.dto.StorageChangeSet;

import java.util.List;
import java.util.Map;

public interface StateRPC {

    @JsonRpcMethod("state_call")
    void stateCall(final String method, final String data, final String blockHashHex);

    @JsonRpcMethod("state_getPairs")
    @UnsafeRpcMethod
    String[][] stateGetPairs(final String prefix, final String blockHashHex);

    @JsonRpcMethod("state_getKeysPaged")
    @UnsafeRpcMethod
    List<String> stateGetKeysPaged(final String prefix, int limit, String keyHex, final String blockHashHex);

    @JsonRpcMethod("state_getStorage")
    String stateGetStorage(final String keyHex, final String blockHashHex);

    @JsonRpcMethod("state_getStorageHash")
    String stateGetStorageHash(final String keyHex, final String blockHashHex);

    @JsonRpcMethod("state_getStorageSize")
    String stateGetStorageSize(final String keyHex, final String blockHashHex);

    @JsonRpcMethod("state_getStorageSizeAt")
    String stateGetStorageSizeAt(final String keyHex, final String blockHashHex); //Alias for childstate_getStorageSizeAt

    @JsonRpcMethod("state_getMetadata")
    String stateGetMetadata(final String blockHashHex);

    @JsonRpcMethod("state_getRuntimeVersion")
    String stateGetRuntimeVersion(final String blockHashHex);

    @JsonRpcMethod("state_queryStorage")
    @UnsafeRpcMethod
    List<StorageChangeSet> stateQueryStorage(final List<String> keyHex, final String startBlockHash, final String endBlockHash);

    @JsonRpcMethod("state_queryStorageAt")
    @UnsafeRpcMethod
    List<StorageChangeSet> stateQueryStorageAt(final List<String> keyHex, final String startBlockHash);

    @JsonRpcMethod("state_getReadProof")
    Map<String, Object> stateGetReadProof(final List<String> keyHex, final String blockHashHex);

}

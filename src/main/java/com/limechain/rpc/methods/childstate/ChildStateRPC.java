package com.limechain.rpc.methods.childstate;

import com.googlecode.jsonrpc4j.JsonRpcMethod;
import com.limechain.rpc.config.UnsafeRpcMethod;

import java.util.List;

public interface ChildStateRPC {
    
    @JsonRpcMethod("childstate_getKeys")
    @UnsafeRpcMethod
    List<String> childStateGetKeys(final String childKeyHex, final String prefix, final String blockHashHex);

    @JsonRpcMethod("childstate_getStorage")
    String childStateGetStorage(final String childKeyHex, final String keyHex, final String blockHashHex);

    @JsonRpcMethod("childstate_getStorageHash")
    String childStateGetStorageHash(final String childKeyHex, final String keyHex, final String blockHashHex);

    @JsonRpcMethod("childstate_getStorageSize")
    String childStateGetStorageSize(final String childKeyHex, final String keyHex, final String blockHashHex);

}

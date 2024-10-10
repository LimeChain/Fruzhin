package com.limechain.rpc.config;

public enum RpcMethods {
    /**
     * Allow only a safe subset of RPC methods
     */
    SAFE,
    /**
     * Expose every RPC method (even potentially unsafe ones)
     */
    UNSAFE
}

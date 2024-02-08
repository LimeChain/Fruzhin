package com.limechain.rpc.config;

public enum RpcMethods {
    /**
     * Expose every RPC method only when RPC is listening on
     * `localhost`, otherwise serve only safe RPC methods
     */
    AUTO,
    /**
     * Allow only a safe subset of RPC methods
     */
    SAFE,
    /**
     * Expose every RPC method (even potentially unsafe ones)
     */
    UNSAFE
}

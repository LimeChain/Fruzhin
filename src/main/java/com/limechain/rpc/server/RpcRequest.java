package com.limechain.rpc.server;

import lombok.Getter;

/**
 * Represents a jsonrpc request body
 */
@Getter
public class RpcRequest {
    private String id;
    private String method;
    private String[] params;
    private String jsonrpc;

}

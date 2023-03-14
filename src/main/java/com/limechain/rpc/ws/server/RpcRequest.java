package com.limechain.rpc.ws.server;

import lombok.Getter;

@Getter
public class RpcRequest {
    private String id;
    private String method;
    private String[] params;
    private String jsonrpc;

}

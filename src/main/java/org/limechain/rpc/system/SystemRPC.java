package org.limechain.rpc.system;

import com.googlecode.jsonrpc4j.JsonRpcService;

@JsonRpcService("/")
public interface SystemRPC {
    String system_name ();
}

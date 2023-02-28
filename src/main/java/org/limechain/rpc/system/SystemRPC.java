package org.limechain.rpc.system;

import com.googlecode.jsonrpc4j.JsonRpcService;

import java.util.Map;

@JsonRpcService("/")
public interface SystemRPC {
    String system_name ();

    String system_version ();

    String system_chain ();

    String system_chainType ();

    //TODO: Change return type to be specific class
    Map<String, Object> system_properties ();

    String[] system_nodeRoles ();


}

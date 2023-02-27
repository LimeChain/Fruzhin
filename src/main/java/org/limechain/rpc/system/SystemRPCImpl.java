package org.limechain.rpc.system;

import com.googlecode.jsonrpc4j.spring.AutoJsonRpcServiceImpl;
import org.springframework.stereotype.Service;

@Service
@AutoJsonRpcServiceImpl
public class SystemRPCImpl implements SystemRPC {

    @Override
    public String system_name () {
        return "Java Host";
    }

}

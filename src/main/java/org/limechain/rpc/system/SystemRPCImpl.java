package org.limechain.rpc.system;

import com.googlecode.jsonrpc4j.spring.AutoJsonRpcServiceImpl;
import org.limechain.chain.ChainService;
import org.limechain.config.SystemInfo;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@AutoJsonRpcServiceImpl
public class SystemRPCImpl implements SystemRPC {

    private final ChainService chainService;
    private final SystemInfo systemInfo;

    public SystemRPCImpl (ChainService chainService, SystemInfo systemInfo) {
        this.chainService = chainService;
        this.systemInfo = systemInfo;
    }

    @Override
    public String system_name () {
        return this.systemInfo.hostName;
    }

    @Override
    public String system_version () {
        return this.systemInfo.hostVersion;
    }

    @Override
    public String system_chain () {
        return this.chainService.genesis.name;
    }

    @Override
    public String system_chainType () {
        return this.chainService.genesis.chainType;
    }

    @Override
    public Map<String, Object> system_properties () {
        return this.chainService.genesis.properties;
    }

    @Override
    public String[] system_nodeRoles () {
        return new String[]{this.systemInfo.role};
    }

}

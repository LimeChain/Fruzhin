package org.limechain.rpc;

import com.googlecode.jsonrpc4j.spring.AutoJsonRpcServiceImplExporter;
import org.limechain.chain.ChainService;
import org.limechain.config.HostConfig;
import org.limechain.config.SystemInfo;
import org.limechain.rpc.ws.client.WSClient;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RPCConfig {

    @Bean
    public static AutoJsonRpcServiceImplExporter autoJsonRpcServiceImplExporter () {
        AutoJsonRpcServiceImplExporter exp = new AutoJsonRpcServiceImplExporter();
        // in here you can provide custom HTTP status code providers etc. eg:
        // exp.setHttpStatusCodeProvider();
        // exp.setErrorResolver();
        return exp;
    }

    @Bean
    public HostConfig appConfig (ApplicationArguments arguments) {
        String[] commandLineArguments = arguments.getSourceArgs();
        System.out.println(commandLineArguments);
        return new HostConfig(commandLineArguments);
    }

    @Bean
    public ChainService chainService (HostConfig hostConfig) {
        return new ChainService(hostConfig);
    }

    @Bean
    public SystemInfo systemInfo (HostConfig hostConfig) {
        return new SystemInfo();
    }

    @Bean
    public WSClient wsClient () {
        return new WSClient();
    }

}

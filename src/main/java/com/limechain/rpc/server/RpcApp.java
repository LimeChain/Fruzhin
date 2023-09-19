package com.limechain.rpc.server;

import com.limechain.config.SystemInfo;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import java.util.Collections;

/**
 * Main RPC Spring application class.
 */
@SpringBootApplication
@ComponentScan(basePackages = {
        "com.limechain.rpc.config",
        "com.limechain.rpc.methods",
        "com.limechain.rpc.server",
        "com.limechain.storage"
})
public class RpcApp {

    /**
     * Port the Spring app will run on
     */
    private final String serverPort = "9922";

    /**
     * Spring application context
     */
    private ConfigurableApplicationContext springCtx;

    /**
     * Starts the Spring application.
     *
     * @param cliArgs arguments that will be passed as
     *                ApplicationArguments to {@link com.limechain.rpc.config.CommonConfig}.
     * @see com.limechain.rpc.config.CommonConfig#hostConfig(ApplicationArguments)
     */
    public void start(String[] cliArgs) {
        SpringApplication app = new SpringApplication(RpcApp.class);
        app.setDefaultProperties(Collections.singletonMap("server.port", serverPort));
        ConfigurableApplicationContext ctx = app.run(cliArgs);
        ctx.getBean(SystemInfo.class).logSystemInfo();
        this.springCtx = ctx;
    }

    /**
     * Shuts down the spring application as well as any services that it's using
     */
    public void stop() {
        if (this.springCtx != null) {
            this.springCtx.stop();
        }
    }

}

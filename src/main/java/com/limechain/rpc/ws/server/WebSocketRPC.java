package com.limechain.rpc.ws.server;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import java.util.Collections;

/**
 * Main WS Spring application class. Starts one of the two Spring applications.
 * <p>
 * Reason for having two Spring applications is because we have to expose both http and ws
 * <a href="https://wiki.polkadot.network/docs/build-node-interaction#polkadot-rpc">endpoints on different ports</a>
 * which isn't possible with a single Spring app.
 */
@SpringBootApplication
@ComponentScan(basePackages = {
        "com.limechain.rpc.config",
        "com.limechain.rpc.methods",
        "com.limechain.rpc.ws.server",
        "com.limechain.storage"
})
public class WebSocketRPC {

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
        SpringApplication app = new SpringApplication(WebSocketRPC.class);
        app.setDefaultProperties(Collections.singletonMap("server.port", serverPort));
        ConfigurableApplicationContext ctx = app.run(cliArgs);
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

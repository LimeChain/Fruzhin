package com.limechain.rpc.server;

import com.limechain.config.SystemInfo;
import com.limechain.network.Network;
import com.limechain.sync.warpsync.WarpSyncMachine;
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
     * The reference to the underlying SpringApplication
     */
    private final SpringApplication app;

    public RpcApp() {
        this.app = new SpringApplication(RpcApp.class);
        app.setDefaultProperties(Collections.singletonMap("server.port", serverPort));
    }

    /**
     * Spring application context
     */
    private ConfigurableApplicationContext springCtx;

    /**
     * Starts the Spring application.
     *
     * @param cliArgs arguments that will be passed as
     *                ApplicationArguments to {@link com.limechain.rpc.config.CommonConfig}.
     * @see com.limechain.rpc.config.CommonConfig#hostConfig(com.limechain.cli.CliArguments)
     */
    public void start(String[] cliArgs) {
        ConfigurableApplicationContext ctx = app.run(cliArgs);
        ctx.getBean(SystemInfo.class).logSystemInfo();
        this.springCtx = ctx;
    }

    /**
     * Shuts down the spring application as well as any services that it's using
     */
    public void stop() {
        // TODO: This is untestable with our current design... but do we need to test it really?
        //  (I mean verifying that everything necessary has been stopped)

        // TODO: Think of a way to make those beans lifecycle-aware so that stopping the context would propagate
        //  to stopping the necessary instances
        //  Perhaps Spring can handle this for us instead of us manually stopping the beans?
        AppBean.getBean(WarpSyncMachine.class).stop();
        AppBean.getBean(Network.class).stop();

        if (this.springCtx != null) {
            this.springCtx.stop();
        }
    }

}

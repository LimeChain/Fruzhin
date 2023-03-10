package com.limechain.rpc.ws.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import java.util.Collections;

@SpringBootApplication
@ComponentScan(basePackages = {
        "com.limechain.rpc.config",
        "com.limechain.rpc.methods",
        "com.limechain.rpc.ws.server",
        "com.limechain.storage"
})
public class WebSocketRPC {
    private ConfigurableApplicationContext springCtx;


    public void start(String[] cliArgs) {
        SpringApplication app = new SpringApplication(WebSocketRPC.class);
        app.setDefaultProperties(Collections.singletonMap("server.port", "9922"));
        ConfigurableApplicationContext ctx = app.run(cliArgs);
        this.springCtx = ctx;
    }

    public void stop() {
        if (this.springCtx != null) {
            this.springCtx.stop();
        }
    }

}

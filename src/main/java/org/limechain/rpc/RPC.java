package org.limechain.rpc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
//@ImportResource("./beans.xml")
public class RPC {
    private ConfigurableApplicationContext springCtx;

    public void start () {
        ConfigurableApplicationContext ctx = SpringApplication.run(RPC.class);
        this.springCtx = ctx;
    }

    public void stop () {
        if (this.springCtx != null) {
            this.springCtx.stop();
        }
    }


}

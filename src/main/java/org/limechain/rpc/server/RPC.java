package org.limechain.rpc.server;

import org.rocksdb.RocksDB;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import java.util.Collections;

@SpringBootApplication
@ComponentScan(basePackages = {"org.limechain.methods", "org.limechain.rpc.server"})
public class RPC {
    private ConfigurableApplicationContext springCtx;

    public void start (String[] cliArgs) {
        SpringApplication app = new SpringApplication(RPC.class);
        app.setDefaultProperties(Collections.singletonMap("server.port", "9933"));
        ConfigurableApplicationContext ctx = app.run(cliArgs);
        this.springCtx = ctx;
    }

    public void stop () {
        if (this.springCtx != null) {
            RocksDB db = this.springCtx.getBean(RocksDB.class);
            db.close();
            this.springCtx.stop();
        }
    }


}

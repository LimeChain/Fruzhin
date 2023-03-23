package com.limechain.rpc.http.server;

import org.rocksdb.RocksDB;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Collections;

@SpringBootApplication
@ComponentScan(basePackages = {
        "com.limechain.rpc.config",
        "com.limechain.rpc.methods",
        "com.limechain.rpc.http.server",
        "com.limechain.storage"
})
@EnableScheduling
public class HttpRpc {
    private ConfigurableApplicationContext springCtx;

    public void start(String[] cliArgs) {
        SpringApplication app = new SpringApplication(HttpRpc.class);
        app.setDefaultProperties(Collections.singletonMap("server.port", "9933"));
        ConfigurableApplicationContext ctx = app.run(cliArgs);
        this.springCtx = ctx;
    }

    public void stop() {
        if (this.springCtx != null) {
            RocksDB db = this.springCtx.getBean(RocksDB.class);
            db.close();
            this.springCtx.stop();
        }
    }

}

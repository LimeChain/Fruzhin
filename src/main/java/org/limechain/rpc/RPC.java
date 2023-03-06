package org.limechain.rpc;

import org.rocksdb.RocksDB;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;

@SpringBootApplication
public class RPC {
    private ConfigurableApplicationContext springCtx;

    public void start () {
        ConfigurableApplicationContext ctx = SpringApplication.run(RPC.class);
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

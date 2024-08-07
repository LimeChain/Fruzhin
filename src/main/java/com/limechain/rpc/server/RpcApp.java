package com.limechain.rpc.server;

import com.limechain.utils.DivLogger;

public class RpcApp {

    private static final DivLogger log = new DivLogger();

    public void start() {
        log.log("RpcApp.start() called");
        CommonConfig.start();
    }


}

package com.limechain.rpc.server;

import com.limechain.rpc.config.CommonConfig;

public class RpcApp {

    /**
     * Port the Spring app will run on
     */
    private static final String SERVER_PORT = "9922";


    public RpcApp() {
    }

    public void start() {
        CommonConfig.start();
    }


}

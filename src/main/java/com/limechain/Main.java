package com.limechain;

import com.limechain.client.HostNode;
import com.limechain.config.HostConfig;
import com.limechain.rpc.server.AppBean;
import com.limechain.rpc.server.RpcApp;
import com.limechain.utils.DivLogger;

public class Main {

    private static final DivLogger log = new DivLogger();

    public static void main(String[] args) {
        log.log("Starting LimeChain node...");
        RpcApp rpcApp = new RpcApp();
        rpcApp.start();

        HostConfig hostConfig = AppBean.getBean(HostConfig.class);

        // Figure out what client role we want to start
//        final NodeRole nodeRole = hostConfig.getNodeRole();
        HostNode client;

//        switch (nodeRole) {
//            case LIGHT -> client = new LightClient();
//            case NONE -> {
//                // This shouldn't happen.
//                return;
//            }
//            default -> {
//                log.log(Level.SEVERE, "Node role {0} not yet implemented.", nodeRole);
//                return;
//            }
//        }

        // Start the client
        // NOTE: This starts the beans the client would need - mutates the global context
//        client.start();
//        log.log(Level.INFO, "\uD83D\uDE80Started {0} client!", nodeRole);

    }
}
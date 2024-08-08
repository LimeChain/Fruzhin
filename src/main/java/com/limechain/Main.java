package com.limechain;

import com.limechain.client.HostNode;
import com.limechain.client.LightClient;
import com.limechain.config.HostConfig;
import com.limechain.rpc.server.AppBean;
import com.limechain.rpc.server.RpcApp;
import com.limechain.utils.DivLogger;

import java.util.logging.Level;

public class Main {

    private static final DivLogger log = new DivLogger();

    public static void main(String[] args) {
        log.log("Starting LimeChain node...");
        RpcApp rpcApp = new RpcApp();
        rpcApp.start();

        HostNode client = new LightClient();

        // Start the client
        // NOTE: This starts the beans the client would need - mutates the global context
        client.start();
        log.log(Level.INFO, "\uD83D\uDE80Started light client!");
    }
}
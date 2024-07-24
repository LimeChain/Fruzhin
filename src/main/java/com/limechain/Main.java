package com.limechain;

import com.limechain.client.HostNode;
import com.limechain.client.LightClient;
import com.limechain.config.HostConfig;
import com.limechain.network.protocol.blockannounce.NodeRole;
import com.limechain.rpc.server.AppBean;
import com.limechain.rpc.server.RpcApp;
import lombok.extern.java.Log;
import sun.misc.Signal;

import java.io.IOException;
import java.util.logging.Level;

@Log
public class Main {

    public static void main(String[] args) throws IOException {
        // Instantiate and start the spring application, so we get the global context
        RpcApp rpcApp = new RpcApp();
        rpcApp.start();

        HostConfig hostConfig = AppBean.getBean(HostConfig.class);

        // Figure out what client role we want to start
        final NodeRole nodeRole = hostConfig.getNodeRole();
        HostNode client;

        switch (nodeRole) {
            case LIGHT -> client = new LightClient();
            case NONE -> {
                // This shouldn't happen.
                return;
            }
            default -> {
                log.log(Level.SEVERE, "Node role {0} not yet implemented.", nodeRole);
                return;
            }
        }

        // Start the client
        // NOTE: This starts the beans the client would need - mutates the global context
        client.start();
        log.log(Level.INFO, "\uD83D\uDE80Started {0} client!", nodeRole);

        Signal.handle(new Signal("INT"), signal -> {
            log.log(Level.INFO, "\uD83D\uDED1Stopped {0} client!", nodeRole);
            System.exit(0);
        });
    }
}
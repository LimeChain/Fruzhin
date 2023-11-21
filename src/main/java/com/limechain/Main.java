package com.limechain;

import com.limechain.config.HostConfig;
import com.limechain.lightclient.FullClient;
import com.limechain.lightclient.LightClient;
import com.limechain.lightclient.NodeClient;
import com.limechain.network.protocol.blockannounce.NodeRole;
import com.limechain.rpc.server.AppBean;
import com.limechain.rpc.server.RpcApp;
import lombok.extern.java.Log;
import sun.misc.Signal;

import java.util.logging.Level;

@Log
public class Main {

    public static void main(String[] args) {
        // Instantiate and start the spring application, so we get teh global context
        RpcApp rpcApp = new RpcApp();
        rpcApp.start(args);

        // Figure out what client role we want to start
        HostConfig hostConfig = AppBean.getBean(HostConfig.class);
        final NodeRole nodeRole = hostConfig.getNodeRole();
        NodeClient client;

        switch (nodeRole) {
            case FULL -> {
                client = new FullClient();
            }
            case LIGHT -> {
                client = new LightClient();
            }
            case NONE -> {
                // This shouldn't happen.
                // TODO: don't use this enum for the CLI NodeRole option
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
            rpcApp.stop(); // NOTE: rpcApp is responsible for stopping everything that could've been started

            // TODO: Maybe think of another place to hold the logic below
            log.log(Level.INFO, "\uD83D\uDED1Stopped {0} client!", nodeRole);
            System.exit(0);
        });
    }
}